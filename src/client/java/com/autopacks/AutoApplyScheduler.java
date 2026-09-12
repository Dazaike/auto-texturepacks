package com.autopacks;

import net.minecraft.client.Minecraft;

/**
 * One-shot delayed auto-apply. Armed on game start, counted down on client
 * ticks, fired exactly once per launch. No screen or world gating: once the
 * timer elapses the apply runs wherever the player is (TitleScreen-only
 * gating misses fast logins).
 */
public final class AutoApplyScheduler {
	private static boolean fired = false;
	private static int ticksLeft = -1;

	private AutoApplyScheduler() {
	}

	/** Called from CLIENT_STARTED. Arms only when there is something to restore. */
	public static void onClientStarted(Minecraft client) {
		fired = false;
		AutoPacksConfig config = AutoPacksConfig.get();
		if (!config.enabled || config.snapshot.isEmpty()) {
			ticksLeft = -1;
			return;
		}
		ticksLeft = (int) Math.round(config.clampedDelaySeconds() * 20.0);
	}

	/**
	 * Called after a manual apply (e.g. the "Apply now" button) so a still
	 * -pending auto-apply timer never redundantly re-fires later this
	 * launch. Without this, clicking Apply now shortly before the scheduled
	 * delay elapses causes a second, effect-less reload (visible as the
	 * pack-reload overlay flashing twice).
	 */
	public static void cancelPending() {
		ticksLeft = -1;
		fired = true;
	}

	/** Called from END_CLIENT_TICK. */
	public static void onEndTick(Minecraft client) {
		if (fired || ticksLeft < 0) {
			return;
		}
		if (client == null) {
			// Defensive; effectively unreachable on END_CLIENT_TICK.
			ticksLeft = 20;
			return;
		}
		if (ticksLeft > 0) {
			ticksLeft--;
			if (ticksLeft > 0) {
				return;
			}
		}
		fired = true;
		ticksLeft = -1;
		PackApplier.apply(client, AutoPacksConfig.get().snapshot, true);
	}
}
