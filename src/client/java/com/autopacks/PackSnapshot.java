package com.autopacks;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Captures the enabled pack list verbatim (order preserved, never sorted or reversed).
 *
 * Required packs (vanilla plus every mod's own resource pack, e.g. all the
 * "fabric-*" entries) are excluded: {@link PackRepository#setSelected} force
 * re-adds every required pack regardless of what is passed in, so tracking
 * them here only pollutes the snapshot and pack counts with dozens of
 * entries the player never toggled and can never turn off.
 */
public final class PackSnapshot {
	private PackSnapshot() {
	}

	public static List<String> currentSelectedIds(Minecraft client) {
		PackRepository repository = client.getResourcePackRepository();
		Set<String> requiredIds = requiredIds(repository);
		List<String> ids = new ArrayList<>();
		for (String id : repository.getSelectedIds()) {
			if (!requiredIds.contains(id)) {
				ids.add(id);
			}
		}
		return ids;
	}

	/** Ids of packs the engine always force-selects; never optional/toggleable. */
	static Set<String> requiredIds(PackRepository repository) {
		Set<String> required = new HashSet<>();
		for (Pack pack : repository.getAvailablePacks()) {
			if (pack.isRequired()) {
				required.add(pack.getId());
			}
		}
		return required;
	}

	public static void captureNow(Minecraft client) {
		AutoPacksConfig config = AutoPacksConfig.get();
		config.snapshot = currentSelectedIds(client);
		config.save();
		AutoPacksMod.LOGGER.info("[AutoPacks] Snapshot captured: {} pack(s)", config.snapshot.size());
	}
}
