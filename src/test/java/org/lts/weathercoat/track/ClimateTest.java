package org.lts.weathercoat.track;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClimateTest {
    @Test
    void temperateShadeDriesAtTheBaseRate() {
        assertEquals(1.0F, Climate.dryFactor(0.8F, false, false, false));
    }

    @Test
    void desertSunAndFireDryFaster() {
        float desert = Climate.dryFactor(2.0F, false, false, false);
        float sunnyDesert = Climate.dryFactor(2.0F, false, true, false);
        float campfire = Climate.dryFactor(0.8F, false, false, true);
        assertTrue(desert > 1.0F);
        assertTrue(sunnyDesert > desert);
        assertTrue(campfire > desert);
    }

    @Test
    void swampsDrySlower() {
        assertTrue(Climate.dryFactor(0.8F, true, false, false) < 1.0F);
    }

    @Test
    void snowMeltsFasterWhenWarmHotOrShaken() {
        float cold = Climate.meltFactor(false, 0.0F, false, false, false);
        assertEquals(1.0F, cold);
        assertTrue(Climate.meltFactor(true, 0.8F, false, false, false) > cold);
        assertTrue(Climate.meltFactor(false, 0.0F, false, false, true) > cold);
        assertTrue(Climate.meltFactor(false, 0.0F, false, true, false) > Climate.meltFactor(true, 0.8F, false, false, false));
    }

    @Test
    void frostStageFollowsFreezing() {
        assertEquals(0, Climate.frostStage(0.0F, 4));
        assertEquals(1, Climate.frostStage(0.01F, 4));
        assertEquals(2, Climate.frostStage(0.5F, 4));
        assertEquals(4, Climate.frostStage(1.0F, 4));
        assertEquals(4, Climate.frostStage(3.0F, 4));
        assertEquals(0, Climate.frostStage(Float.NaN, 4));
    }
}
