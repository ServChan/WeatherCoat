package org.lts.weathercoat.track;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JoinCatchUpTest {
    @Test
    void rainAlreadyFallingOnJoinCatchesUpOnce() {
        JoinCatchUp catchUp = new JoinCatchUp();
        assertFalse(catchUp.tick(0.0F));
        assertTrue(catchUp.tick(1.0F));
        assertFalse(catchUp.tick(1.0F));
    }

    @Test
    void rainStartingLaterDoesNotCatchUp() {
        JoinCatchUp catchUp = new JoinCatchUp();
        float rain = 0.0F;
        for (int tick = 0; tick < 200; tick++) {
            rain = Math.min(1.0F, rain + 0.01F);
            assertFalse(catchUp.tick(rain), "tick " + tick);
        }
    }

    @Test
    void resetArmsItAgain() {
        JoinCatchUp catchUp = new JoinCatchUp();
        for (int tick = 0; tick < 100; tick++) {
            catchUp.tick(0.0F);
        }
        catchUp.reset();
        assertTrue(catchUp.tick(1.0F));
    }
}
