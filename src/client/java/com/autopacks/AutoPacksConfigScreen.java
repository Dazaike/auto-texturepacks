package com.autopacks;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Vanilla-only settings screen. Widgets write to working copies;
 * {@code Done} persists them to {@link AutoPacksConfig}.
 */
public class AutoPacksConfigScreen extends Screen {
	private static final int ROW_WIDTH = 310;
	private static final int MAX_SNAPSHOT_LINES = 10;

	private final Screen parent;

	private boolean enabled;
	private double delaySeconds;
	private boolean notifyOnApply;
	private boolean skipIfAlreadyApplied;

	private MultiLineTextWidget snapshotWidget;

	public AutoPacksConfigScreen(Screen parent) {
		super(Component.literal("Auto Packs Settings"));
		this.parent = parent;
		AutoPacksConfig config = AutoPacksConfig.get();
		this.enabled = config.enabled;
		this.delaySeconds = config.clampedDelaySeconds();
		this.notifyOnApply = config.notifyOnApply;
		this.skipIfAlreadyApplied = config.skipIfAlreadyApplied;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int left = centerX - ROW_WIDTH / 2;

		StringWidget title = new StringWidget(this.title, this.font);
		title.setX(centerX - title.getWidth() / 2);
		title.setY(16);
		this.addRenderableOnly(title);

		this.addRenderableWidget(Checkbox.builder(Component.literal("Enabled"), this.font)
			.pos(left, 40)
			.maxWidth(ROW_WIDTH)
			.selected(this.enabled)
			.onValueChange((checkbox, selected) -> this.enabled = selected)
			.build());

		this.addRenderableWidget(new DelaySlider(left, 64, ROW_WIDTH));

		this.addRenderableWidget(Checkbox.builder(Component.literal("Show chat/toast notification"), this.font)
			.pos(left, 88)
			.maxWidth(ROW_WIDTH)
			.selected(this.notifyOnApply)
			.onValueChange((checkbox, selected) -> this.notifyOnApply = selected)
			.build());

		this.addRenderableWidget(Checkbox.builder(Component.literal("Skip if packs already match"), this.font)
			.pos(left, 112)
			.maxWidth(ROW_WIDTH)
			.selected(this.skipIfAlreadyApplied)
			.onValueChange((checkbox, selected) -> this.skipIfAlreadyApplied = selected)
			.build());

		StringWidget listHeader = new StringWidget(Component.literal("Saved pack order (top = highest priority):"), this.font);
		listHeader.setX(left);
		listHeader.setY(140);
		this.addRenderableOnly(listHeader);

		this.snapshotWidget = new MultiLineTextWidget(snapshotComponent(), this.font);
		this.snapshotWidget.setMaxWidth(ROW_WIDTH);
		this.snapshotWidget.setMaxRows(MAX_SNAPSHOT_LINES + 1);
		this.snapshotWidget.setX(left);
		this.snapshotWidget.setY(154);
		this.addRenderableOnly(this.snapshotWidget);

		int actionRowY = this.height - 56;
		this.addRenderableWidget(Button.builder(Component.literal("Use current pack order"), button -> {
			PackSnapshot.captureNow(this.minecraft);
			this.snapshotWidget.setMessage(snapshotComponent());
		}).bounds(left, actionRowY, 160, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Apply now"), button ->
			PackApplier.apply(this.minecraft, AutoPacksConfig.get().snapshot, false)
		).bounds(left + 165, actionRowY, ROW_WIDTH - 165, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
			AutoPacksConfig config = AutoPacksConfig.get();
			config.enabled = this.enabled;
			config.delaySeconds = this.delaySeconds;
			config.notifyOnApply = this.notifyOnApply;
			config.skipIfAlreadyApplied = this.skipIfAlreadyApplied;
			config.save();
			this.minecraft.setScreen(this.parent);
		}).bounds(centerX - 100, this.height - 30, 200, 20).build());
	}

	private static Component snapshotComponent() {
		List<String> snapshot = AutoPacksConfig.get().snapshot;
		if (snapshot.isEmpty()) {
			return Component.literal("No saved packs yet");
		}
		StringBuilder lines = new StringBuilder();
		int shown = Math.min(snapshot.size(), MAX_SNAPSHOT_LINES);
		for (int i = 0; i < shown; i++) {
			if (i > 0) {
				lines.append('\n');
			}
			lines.append(snapshot.get(i));
		}
		if (snapshot.size() > shown) {
			lines.append('\n').append('+').append(snapshot.size() - shown).append(" more");
		}
		return Component.literal(lines.toString());
	}

	private static String formatDelay(double delaySeconds) {
		if (delaySeconds == Math.floor(delaySeconds)) {
			return ((int) delaySeconds) + "s after game loads";
		}
		return delaySeconds + "s after game loads";
	}

	private final class DelaySlider extends AbstractSliderButton {
		DelaySlider(int x, int y, int width) {
			super(x, y, width, 20, Component.empty(), delaySeconds / AutoPacksConfig.MAX_DELAY_SECONDS);
			updateMessage();
		}

		private double sliderSeconds() {
			return Math.round(this.value * AutoPacksConfig.MAX_DELAY_SECONDS * 2.0) / 2.0;
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(formatDelay(sliderSeconds())));
		}

		@Override
		protected void applyValue() {
			delaySeconds = sliderSeconds();
		}
	}
}
