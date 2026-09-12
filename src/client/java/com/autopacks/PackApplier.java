package com.autopacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Order-preserving single-reload pack apply. Always runs on the client thread.
 */
public final class PackApplier {
	private PackApplier() {
	}

	public static void apply(Minecraft client, List<String> snapshot, boolean isAuto) {
		if (client == null) {
			return;
		}
		if (!isAuto) {
			// A manual apply makes the pending scheduled auto-apply moot;
			// letting it still fire later redundantly reloads (visible as
			// the pack overlay flashing a second time for no reason).
			AutoApplyScheduler.cancelPending();
		}
		if (!client.isSameThread()) {
			List<String> copy = snapshot == null ? List.of() : new ArrayList<>(snapshot);
			client.execute(() -> apply(client, copy, isAuto));
			return;
		}

		AutoPacksConfig config = AutoPacksConfig.get();
		try {
			if (snapshot == null || snapshot.isEmpty()) {
				AutoPacksMod.LOGGER.info("[AutoPacks] No saved packs, nothing to apply.");
				return;
			}

			PackRepository repository = client.getResourcePackRepository();
			repository.reload();

			// Required packs (vanilla + every mod's own resource pack) are
			// force re-added by setSelected regardless of what we pass in;
			// excluding them here keeps counts/comparisons meaningful
			// (otherwise "48 built-in packs" swamps the real optional ones).
			Set<String> requiredIds = PackSnapshot.requiredIds(repository);
			Set<String> available = new HashSet<>();
			for (String id : repository.getAvailableIds()) {
				if (!requiredIds.contains(id)) {
					available.add(id);
				}
			}

			List<String> target = new ArrayList<>(snapshot.size());
			List<String> missing = new ArrayList<>();
			for (String id : snapshot) {
				if (available.contains(id)) {
					target.add(id);
				} else {
					missing.add(id);
				}
			}

			List<String> current = new ArrayList<>();
			for (String id : repository.getSelectedIds()) {
				if (!requiredIds.contains(id)) {
					current.add(id);
				}
			}
			if (config.skipIfAlreadyApplied && current.equals(target)) {
				AutoPacksMod.LOGGER.info("[AutoPacks] Packs already match snapshot, skipping reload.");
				AutoPacksMod.LOGGER.debug("[AutoPacks] notifyOnApply={}", config.notifyOnApply);
				return;
			}

			repository.setSelected(target);
			client.options.updateResourcePacks(repository);
			client.options.save();
			// Force the reload instead of the deferrable public API: see
			// ForcedReload for why (avoids an unexplained second reload
			// flash caused by Minecraft's own boot-time defer behavior).
			ForcedReload.run(client);

			AutoPacksMod.LOGGER.info("[AutoPacks] {} applied {} pack(s){}",
				isAuto ? "Auto-apply" : "Manual apply",
				target.size(),
				missing.isEmpty() ? "" : ", missing: " + missing);

			if (config.notifyOnApply) {
				if (missing.isEmpty()) {
					SystemToast.addOrUpdate(client.getToastManager(),
						SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
						Component.literal("[AutoPacks] Packs applied"),
						Component.literal(target.size() + " pack(s) enabled in saved order"));
				} else {
					SystemToast.addOrUpdate(client.getToastManager(),
						SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
						Component.literal("[AutoPacks] Packs applied (" + target.size() + ")"),
						Component.literal("Missing: " + summarize(missing)));
				}
			}
		} catch (Exception e) {
			AutoPacksMod.LOGGER.error("[AutoPacks] Failed to apply packs", e);
			if (config.notifyOnApply) {
				SystemToast.addOrUpdate(client.getToastManager(),
					SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
					Component.literal("[AutoPacks] Apply failed"),
					Component.literal(String.valueOf(e.getMessage())));
			}
		}
	}

	private static String summarize(List<String> ids) {
		if (ids.size() <= 3) {
			return String.join(", ", ids);
		}
		return String.join(", ", ids.subList(0, 3)) + ", +" + (ids.size() - 3) + " more";
	}
}
