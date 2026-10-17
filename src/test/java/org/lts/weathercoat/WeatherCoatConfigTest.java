package org.lts.weathercoat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WeatherCoatConfigTest {
    @TempDir
    Path dir;

    @Test
    void sanitizeClampsEveryRange() {
        WeatherCoatConfig config = new WeatherCoatConfig();
        config.stages = 9;
        config.wetSecondsPerStage = 0;
        config.drySecondsPerStage = 100_000;
        config.snowSecondsPerStage = -5;
        config.meltSecondsPerStage = 1;
        config.maxDistance = 4;
        WeatherCoatConfig clean = WeatherCoatConfig.sanitize(config);
        assertEquals(WeatherCoatConfig.MAX_STAGES, clean.stages);
        assertEquals(2, clean.wetSecondsPerStage);
        assertEquals(600, clean.drySecondsPerStage);
        assertEquals(2, clean.snowSecondsPerStage);
        assertEquals(2, clean.meltSecondsPerStage);
        assertEquals(16, clean.maxDistance);
    }

    @Test
    void defaultsStayInsideTheAllowedStageRange() {
        WeatherCoatConfig config = WeatherCoatConfig.defaults();
        assertTrue(config.stages >= WeatherCoatConfig.MIN_STAGES && config.stages <= WeatherCoatConfig.MAX_STAGES);
    }

    @Test
    void writesAndReadsBack() throws Exception {
        Path file = dir.resolve("weathercoat.json");
        WeatherCoatConfig config = new WeatherCoatConfig();
        config.stages = 3;
        config.affectPlayers = false;
        WeatherCoatConfig.writeAtomically(file, config, true);
        WeatherCoatConfig loaded = WeatherCoatConfig.load(file);
        assertEquals(3, loaded.stages);
        assertFalse(loaded.affectPlayers);
    }

    @Test
    void brokenFileFallsBackToBackup() throws Exception {
        Path file = dir.resolve("weathercoat.json");
        WeatherCoatConfig config = new WeatherCoatConfig();
        config.stages = 5;
        WeatherCoatConfig.writeAtomically(file, config, true);
        WeatherCoatConfig.writeAtomically(file, config, true);
        Files.writeString(file, "{ not json");
        assertEquals(5, WeatherCoatConfig.load(file).stages);
    }
}
