package com.autopacks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoPacksMod implements ClientModInitializer {
	public static final String MOD_ID = "auto-texturepacks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			// Scheduler first: it must see the pre-bootstrap snapshot, so a
			// first run (empty snapshot) arms nothing and never reloads.
			AutoApplyScheduler.onClientStarted(client);
			// First-run bootstrap: remember whatever is enabled now so there
			// is something to restore on later launches. Never reloads here.
			if (AutoPacksConfig.get().snapshot.isEmpty()) {
				PackSnapshot.captureNow(client);
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(AutoApplyScheduler::onEndTick);

		// No CLIENT_STOPPING auto-capture: it would silently overwrite a
		// deliberately saved snapshot the moment the live selection differs
		// (e.g. the player left packs unselected to test restore-on-launch),
		// destroying the saved order with no warning. The snapshot changes
		// only via the explicit "Use current pack order" button.

		LOGGER.info("[AutoPacks] initialized");
	}
}
