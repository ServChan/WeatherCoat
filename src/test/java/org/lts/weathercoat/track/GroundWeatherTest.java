package org.lts.weathercoat.track;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.lts.weathercoat.paint.GroundPainter;

class GroundWeatherTest {
    @Test
    void puddlesAppearAfterSomeRainAndFillUp() {
        GroundWeather ground = new GroundWeather();
        ground.advance(5.0F, 1.0F, 1.0F, 1.0F, true, true);
        assertEquals(0, ground.puddleStage());
        ground.advance(GroundWeather.PUDDLE_FILL_SECONDS, 1.0F, 1.0F, 1.0F, true, true);
        assertEquals(GroundPainter.STAGES, ground.puddleStage());
    }

    @Test
    void puddlesDryFasterInTheSun() {
        GroundWeather shade = new GroundWeather();
        GroundWeather sun = new GroundWeather();
        shade.advance(GroundWeather.PUDDLE_FILL_SECONDS, 1.0F, 1.0F, 1.0F, true, true);
        sun.advance(GroundWeather.PUDDLE_FILL_SECONDS, 1.0F, 1.0F, 1.0F, true, true);
        shade.advance(60.0F, 0.0F, 1.0F, 1.0F, true, true);
        sun.advance(60.0F, 0.0F, 2.0F, 1.0F, true, true);
        assertTrue(sun.puddles() < shade.puddles());
        sun.advance(GroundWeather.PUDDLE_DRY_SECONDS, 0.0F, 1.0F, 1.0F, true, true);
        assertEquals(0, sun.puddleStage());
    }

    @Test
    void turningOffClearsAtOnce() {
        GroundWeather ground = new GroundWeather();
        ground.advance(GroundWeather.SNOW_FILL_SECONDS, 1.0F, 1.0F, 1.0F, true, true);
        assertEquals(GroundPainter.STAGES, ground.snowStage());
        ground.advance(0.05F, 1.0F, 1.0F, 1.0F, true, false);
        assertEquals(0, ground.snowStage());
        assertTrue(ground.puddleStage() > 0);
    }

    @Test
    void stageNeverExceedsRange() {
        assertEquals(0, GroundWeather.stage(0.0F));
        assertEquals(GroundPainter.STAGES, GroundWeather.stage(1.0F));
        assertEquals(1, GroundWeather.stage(1.0F / GroundPainter.STAGES));
    }
}
