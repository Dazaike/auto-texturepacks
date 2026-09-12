package com.autopacks;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;

/**
 * Forces an immediate resource-pack reload, bypassing Minecraft's own
 * defer-while-loading-overlay behavior.
 *
 * The public {@code Minecraft#reloadResourcePacks()} always calls the
 * private {@code reloadResourcePacks(boolean force, GameLoadCookie cookie)}
 * overload with {@code force=false}. With force=false, if the boot
 * {@code LoadingOverlay} is still showing (routinely true in the first few
 * seconds after launch, exactly when our auto-apply fires), the reload is
 * silently stashed in a {@code pendingReload} field instead of running, and
 * only flushed later on the next tick after the overlay clears \u2014 producing
 * a second, unexplained pack-reload flash with no mod frames in the stack.
 * Vanilla itself uses {@code force=true} for its own error-recovery reload
 * ({@code clearResourcePacksOnError}), so this is a supported code path, not
 * an internal-only trick; it just isn't exposed publicly.
 *
 * {@code GameLoadCookie} is a private nested type of {@link Minecraft}, so
 * it cannot be named in a normal method signature (a same-package Mixin
 * {@code @Invoker} still fails to compile against it). Reflection sidesteps
 * that entirely: it never needs to spell the type out.
 */
final class ForcedReload {
	private static volatile Method reloadMethod;
	private static volatile boolean unavailable;

	private ForcedReload() {
	}

	/** Runs the reload immediately; falls back to the deferrable public API if reflection ever fails. */
	static void run(Minecraft client) {
		Method method = resolve();
		if (method == null) {
			client.reloadResourcePacks();
			return;
		}
		try {
			method.invoke(client, true, null);
		} catch (ReflectiveOperationException e) {
			AutoPacksMod.LOGGER.warn("[AutoPacks] Forced reload invocation failed, falling back to deferrable reload", e);
			client.reloadResourcePacks();
		}
	}

	private static Method resolve() {
		Method cached = reloadMethod;
		if (cached != null) {
			return cached;
		}
		if (unavailable) {
			return null;
		}
		synchronized (ForcedReload.class) {
			if (reloadMethod != null) {
				return reloadMethod;
			}
			if (unavailable) {
				return null;
			}
			try {
				Class<?> cookieType = Class.forName("net.minecraft.client.Minecraft$GameLoadCookie");
				Method method = Minecraft.class.getDeclaredMethod("reloadResourcePacks", boolean.class, cookieType);
				method.setAccessible(true);
				reloadMethod = method;
				return method;
			} catch (ReflectiveOperationException e) {
				AutoPacksMod.LOGGER.warn("[AutoPacks] Could not resolve forced reload method, using deferrable reload instead", e);
				unavailable = true;
				return null;
			}
		}
	}
}
