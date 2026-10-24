package org.lts.weathercoat.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GroundPainterTest {
    private static final float[] PUDDLES = GroundPainter.puddleField(7L);
    private static final float[] SNOW = GroundPainter.snowField(7L);

    @Test
    void fieldsAreRankedFromZeroToOne() {
        for (float[] field : new float[][] {PUDDLES, SNOW}) {
            float min = 1.0F;
            float max = 0.0F;
            for (float value : field) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            assertEquals(0.0F, min, 1.0E-6F);
            assertEquals(1.0F, max, 1.0E-6F);
        }
    }

    @Test
    void tiledNoiseWrapsAround() {
        int size = GroundPainter.SIZE;
        for (int y = 0; y < size; y += 7) {
            assertEquals(Noise.tiled(0, y, 32, size, 3L), Noise.tiled(size, y, 32, size, 3L), 1.0E-6F);
            assertEquals(Noise.tiled(y, 0, 16, size, 3L), Noise.tiled(y, size, 16, size, 3L), 1.0E-6F);
        }
    }

    @Test
    void puddlesGrowWithStage() {
        int previous = 0;
        for (int stage = 1; stage <= GroundPainter.STAGES; stage++) {
            int water = countAlpha(GroundPainter.paintPuddles(PUDDLES, stage, 0, 7L), 90);
            assertTrue(water > previous, "stage " + stage + " did not grow");
            previous = water;
        }
        assertTrue(previous < PUDDLES.length / 2, "puddles should never flood the whole ground");
    }

    @Test
    void snowCoversMostGroundAtLastStage() {
        int first = countAlpha(GroundPainter.paintSnow(SNOW, 1, 7L), 1);
        int last = countAlpha(GroundPainter.paintSnow(SNOW, GroundPainter.STAGES, 7L), 1);
        assertTrue(first < last);
        assertTrue(last > SNOW.length * 0.9);
        for (int pixel : GroundPainter.paintSnow(SNOW, GroundPainter.STAGES, 7L)) {
            assertTrue((pixel >>> 24) < 255, "snow film must stay see-through");
        }
    }

    @Test
    void stageZeroPaintsNothing() {
        assertEquals(0, countAlpha(GroundPainter.paintPuddles(PUDDLES, 0, 0, 7L), 1));
        assertEquals(0, countAlpha(GroundPainter.paintSnow(SNOW, 0, 7L), 1));
    }

    @Test
    void ripplesOnlyTouchWater() {
        int stage = GroundPainter.STAGES;
        int[] calm = GroundPainter.paintPuddles(PUDDLES, stage, 0, 7L);
        float threshold = 1.0F - GroundPainter.puddleCoverage(stage);
        boolean changed = false;
        for (int frame = 1; frame <= GroundPainter.RIPPLE_FRAMES; frame++) {
            int[] rippled = GroundPainter.paintPuddles(PUDDLES, stage, frame, 7L);
            for (int i = 0; i < calm.length; i++) {
                if (rippled[i] != calm[i]) {
                    changed = true;
                    assertTrue(PUDDLES[i] >= threshold, "ripple outside a puddle");
                }
            }
        }
        assertTrue(changed);
    }

    @Test
    void blockPeaksCoverEveryPaintedPixel() {
        float[] peaks = GroundPainter.blockPeaks(PUDDLES);
        int stage = 3;
        int[] pixels = GroundPainter.paintPuddles(PUDDLES, stage, 0, 7L);
        float cutoff = GroundPainter.puddleCutoff(stage);
        int size = GroundPainter.SIZE;
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] != 0) {
                int block = (i / size / GroundPainter.PIXELS_PER_BLOCK) * GroundPainter.TILE_BLOCKS
                        + (i % size) / GroundPainter.PIXELS_PER_BLOCK;
                assertTrue(peaks[block] >= cutoff, "painted pixel in a skipped block");
            }
        }
    }

    private static int countAlpha(int[] pixels, int minAlpha) {
        int count = 0;
        for (int pixel : pixels) {
            if ((pixel >>> 24) >= minAlpha) {
                count++;
            }
        }
        return count;
    }
}
