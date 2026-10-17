package org.lts.weathercoat.track;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.lts.weathercoat.WeatherCoatConfig;

class ExclusionsTest {
    @Test
    void waterAndFireCreaturesAreSkippedByDefault() {
        WeatherCoatConfig config = WeatherCoatConfig.defaults();
        assertFalse(Exclusions.isAffected(true, false, "minecraft:cod", config));
        assertFalse(Exclusions.isAffected(false, true, "minecraft:blaze", config));
        assertTrue(Exclusions.isAffected(false, false, "minecraft:cow", config));
    }

    @Test
    void defaultListKeepsGuardiansAndSnowGolemsClean() {
        WeatherCoatConfig config = WeatherCoatConfig.defaults();
        assertFalse(Exclusions.isAffected(false, false, "minecraft:guardian", config));
        assertFalse(Exclusions.isAffected(false, false, "minecraft:snow_golem", config));
    }

    @Test
    void togglesCanLetWaterAndFireCreaturesIn() {
        WeatherCoatConfig config = WeatherCoatConfig.defaults();
        config.skipAquatic = false;
        config.skipFireproof = false;
        config.excludedEntities = List.of();
        assertTrue(Exclusions.isAffected(true, false, "minecraft:cod", config));
        assertTrue(Exclusions.isAffected(false, true, "minecraft:blaze", config));
    }

    @Test
    void signatureChangesWithTheRules() {
        WeatherCoatConfig config = WeatherCoatConfig.defaults();
        String before = Exclusions.signature(config);
        config.excludedEntities = List.of("minecraft:cow");
        assertNotEquals(before, Exclusions.signature(config));
    }
}
