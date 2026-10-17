package org.lts.weathercoat.track;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoatProgressTest {
    @Test
    void wetStageGrowsOneStepPerStageDuration() {
        CoatProgress coat = new CoatProgress();
        int[] seen = new int[6];
        for (int i = 0; i < 100; i++) {
            coat.advance(0.05F, 0.0F, 4);
            seen[coat.wetStage()]++;
        }
        assertEquals(4, coat.wetStage());
        for (int stage = 1; stage <= 4; stage++) {
            assertTrue(seen[stage] > 0, "stage " + stage + " was skipped");
        }
    }

    @Test
    void stageDoesNotJumpBackOnFirstDryingTick() {
        CoatProgress coat = new CoatProgress();
        coat.advance(4.0F, 0.0F, 4);
        coat.advance(-0.1F, 0.0F, 4);
        assertEquals(4, coat.wetStage());
        coat.advance(-0.9F, 0.0F, 4);
        assertEquals(3, coat.wetStage());
    }

    @Test
    void meltingWalksDownThroughEveryStage() {
        CoatProgress coat = new CoatProgress();
        coat.advance(5.0F, 5.0F, 5);
        int previous = coat.snowStage();
        while (coat.snowStage() > 0) {
            coat.advance(0.0F, -0.25F, 5);
            assertTrue(previous - coat.snowStage() <= 1, "melting skipped a stage");
            previous = coat.snowStage();
        }
        assertEquals(0.0F, coat.snow());
    }

    @Test
    void progressIsClampedToStageCount() {
        CoatProgress coat = new CoatProgress();
        coat.advance(100.0F, Float.NaN, 3);
        assertEquals(3.0F, coat.wet());
        assertEquals(3, coat.wetStage());
        assertEquals(0.0F, coat.snow());
    }

    @Test
    void fewerStagesClampExistingStage() {
        CoatProgress coat = new CoatProgress();
        coat.advance(5.0F, 0.0F, 5);
        coat.advance(0.0F, 0.0F, 3);
        assertEquals(3, coat.wetStage());
    }

    @Test
    void startUsesWholeStagesOnly() {
        CoatProgress coat = new CoatProgress();
        coat.start(2.7F, 0.4F, 4);
        assertEquals(2, coat.wetStage());
        assertEquals(0, coat.snowStage());
    }

    @Test
    void clearRemovesEverything() {
        CoatProgress coat = new CoatProgress();
        coat.advance(3.0F, 3.0F, 4);
        coat.clear();
        assertTrue(coat.isClean());
    }
}
