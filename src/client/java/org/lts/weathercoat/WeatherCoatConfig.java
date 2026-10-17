package org.lts.weathercoat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

public final class WeatherCoatConfig {
    public static final int MIN_STAGES = 3;
    public static final int MAX_STAGES = 5;
    public static final int MAX_EXCLUDED = 512;
    public static final List<String> DEFAULT_EXCLUDED = List.of(
            "minecraft:guardian", "minecraft:elder_guardian", "minecraft:snow_golem", "minecraft:allay", "minecraft:vex");
    private static final int CURRENT_CONFIG_VERSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile WeatherCoatConfig current = defaults();

    public int configVersion = CURRENT_CONFIG_VERSION;
    public boolean enabled = true;
    public boolean rainEnabled = true;
    public boolean snowEnabled = true;
    public boolean affectPlayers = true;
    public boolean catchUpOnSight = true;
    public int stages = 4;
    public int wetSecondsPerStage = 15;
    public int drySecondsPerStage = 30;
    public int snowSecondsPerStage = 20;
    public int meltSecondsPerStage = 40;
    public int maxDistance = 64;
    public boolean frostEnabled = true;
    public boolean climateEnabled = true;
    public boolean particlesEnabled = true;
    public boolean skipAquatic = true;
    public boolean skipFireproof = true;
    public List<String> excludedEntities = new ArrayList<>(DEFAULT_EXCLUDED);

    public static void load() {
        Path configPath = configPath();
        WeatherCoatConfig primary = read(configPath);
        if (primary != null) {
            boolean migrated = primary.configVersion < CURRENT_CONFIG_VERSION;
            current = sanitize(primary);
            if (migrated) {
                try {
                    writeAtomically(configPath, current, true);
                } catch (IOException exception) {
                    WeatherCoatClient.LOGGER.warn("Could not save migrated {}", configPath, exception);
                }
            }
            return;
        }
        WeatherCoatConfig backup = read(backupPath(configPath));
        current = sanitize(backup == null ? defaults() : backup);
        try {
            writeAtomically(configPath, current, false);
        } catch (IOException exception) {
            WeatherCoatClient.LOGGER.warn("Could not create {}", configPath, exception);
        }
    }

    static WeatherCoatConfig load(Path path) {
        WeatherCoatConfig loaded = read(path);
        if (loaded == null) {
            loaded = read(backupPath(path));
        }
        return sanitize(loaded == null ? defaults() : loaded);
    }

    private static WeatherCoatConfig read(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, WeatherCoatConfig.class);
        } catch (IOException | RuntimeException exception) {
            WeatherCoatClient.LOGGER.warn("Could not read {}", path, exception);
            return null;
        }
    }

    public static WeatherCoatConfig current() {
        return current;
    }

    public static WeatherCoatConfig currentCopy() {
        return copyOf(current);
    }

    public static boolean save(WeatherCoatConfig config) {
        WeatherCoatConfig clean = sanitize(copyOf(config));
        Path configPath = configPath();
        try {
            writeAtomically(configPath, clean, true);
            current = clean;
            return true;
        } catch (IOException exception) {
            WeatherCoatClient.LOGGER.error("Could not save {}", configPath, exception);
            return false;
        }
    }

    public static WeatherCoatConfig defaults() {
        return new WeatherCoatConfig();
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("weathercoat.json");
    }

    static void writeAtomically(Path path, WeatherCoatConfig config, boolean backupExisting) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Path backup = backupPath(path);
        try (Writer writer = Files.newBufferedWriter(temporary)) {
            GSON.toJson(sanitize(copyOf(config)), writer);
        }
        if (backupExisting && Files.isRegularFile(path)) {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path backupPath(Path path) {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    static WeatherCoatConfig sanitize(WeatherCoatConfig config) {
        config.configVersion = CURRENT_CONFIG_VERSION;
        config.stages = clamp(config.stages, MIN_STAGES, MAX_STAGES);
        config.wetSecondsPerStage = clamp(config.wetSecondsPerStage, 2, 300);
        config.drySecondsPerStage = clamp(config.drySecondsPerStage, 2, 600);
        config.snowSecondsPerStage = clamp(config.snowSecondsPerStage, 2, 300);
        config.meltSecondsPerStage = clamp(config.meltSecondsPerStage, 2, 600);
        config.maxDistance = clamp(config.maxDistance, 16, 256);
        config.excludedEntities = normalizeIds(config.excludedEntities);
        return config;
    }

    static List<String> normalizeIds(List<String> ids) {
        if (ids == null) {
            return new ArrayList<>(DEFAULT_EXCLUDED);
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null) {
                continue;
            }
            String clean = id.trim().toLowerCase(Locale.ROOT);
            if (clean.isEmpty()) {
                continue;
            }
            if (clean.indexOf(':') < 0) {
                clean = "minecraft:" + clean;
            }
            if (clean.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") && unique.size() < MAX_EXCLUDED) {
                unique.add(clean);
            }
        }
        return new ArrayList<>(unique);
    }

    private static WeatherCoatConfig copyOf(WeatherCoatConfig config) {
        return GSON.fromJson(GSON.toJson(config), WeatherCoatConfig.class);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
