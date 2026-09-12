package com.autopacks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side config for auto-texturepacks. Plain Gson, no config library.
 * All reads/writes happen on the client thread.
 */
public final class AutoPacksConfig {
	public static final String FILE_NAME = "auto-texturepacks.json";
	public static final double MIN_DELAY_SECONDS = 0.0;
	public static final double MAX_DELAY_SECONDS = 30.0;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static AutoPacksConfig instance;

	public boolean enabled = true;
	public double delaySeconds = 5.0;
	public boolean notifyOnApply = true;
	public boolean skipIfAlreadyApplied = true;
	public List<String> snapshot = new ArrayList<>();

	public static synchronized AutoPacksConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	/** Test/season hook: drop the cached instance so the next {@link #get()} re-reads disk. */
	static synchronized void resetForTests() {
		instance = null;
	}

	public static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	public static AutoPacksConfig load() {
		Path file = configFile();
		if (!Files.isRegularFile(file)) {
			return new AutoPacksConfig();
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			AutoPacksConfig loaded = GSON.fromJson(reader, AutoPacksConfig.class);
			if (loaded == null) {
				throw new JsonSyntaxException("null root");
			}
			loaded.sanitize();
			return loaded;
		} catch (IOException | JsonSyntaxException | IllegalStateException e) {
			// Corrupt file: keep running on defaults. Never delete it here;
			// it is overwritten only on the next explicit save().
			AutoPacksMod.LOGGER.warn("[AutoPacks] Config {} unreadable, using defaults: {}", file, e.toString());
			return new AutoPacksConfig();
		}
	}

	/** Must be called on the client thread only; writes synchronously. */
	public synchronized void save() {
		Path file = configFile();
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			AutoPacksMod.LOGGER.error("[AutoPacks] Failed to save config {}", file, e);
		}
	}

	public double clampedDelaySeconds() {
		return clamp(delaySeconds);
	}

	private void sanitize() {
		delaySeconds = clamp(delaySeconds);
		if (snapshot == null) {
			snapshot = new ArrayList<>();
		} else {
			snapshot = new ArrayList<>(snapshot);
		}
	}

	private static double clamp(double value) {
		if (Double.isNaN(value)) {
			return 5.0;
		}
		return Math.min(MAX_DELAY_SECONDS, Math.max(MIN_DELAY_SECONDS, value));
	}
}
