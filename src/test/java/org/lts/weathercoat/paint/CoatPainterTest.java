package org.lts.weathercoat.paint;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoatPainterTest {
    private static final int SIZE = 64;
    private static final int BASE = 0xFF6A8F3C;

    private static int[] solid(int color) {
        int[] pixels = new int[SIZE * SIZE];
        Arrays.fill(pixels, color);
        return pixels;
    }

    private static FaceMap cube() {
        return FaceMapTest.build(SIZE, SIZE, List.of(
                FaceMapTest.top(0, 0, 0.5F, 0.5F, 0),
                FaceMapTest.side(0.5F, 0, 1, 0.5F, 0, 16),
                FaceMapTest.side(0, 0.5F, 1, 1, 0, 16)));
    }

    private static double brightness(int[] pixels) {
        double sum = 0;
        for (int argb : pixels) {
            sum += ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
        }
        return sum / pixels.length;
    }

    private static boolean isSnow(int argb) {
        return ((argb >> 16) & 0xFF) > 190 && ((argb >> 8) & 0xFF) > 190 && (argb & 0xFF) > 190;
    }

    private static int snowPixels(int[] pixels, int x0, int y0, int x1, int y1) {
        int count = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (isSnow(pixels[y * SIZE + x])) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void cleanCoatKeepsPixels() {
        int[] base = solid(BASE);
        assertArrayEquals(base, CoatPainter.paint(base, cube(), 0.0F, 0.0F, 1L));
    }

    @Test
    void wetterStagesAreDarker() {
        int[] base = solid(BASE);
        FaceMap map = cube();
        double previous = brightness(base);
        for (int stage = 1; stage <= 4; stage++) {
            double now = brightness(CoatPainter.paint(base, map, stage / 4.0F, 0.0F, 7L));
            assertTrue(now < previous, "stage " + stage + " is not darker");
            previous = now;
        }
    }

    @Test
    void snowGrowsWithEveryStage() {
        int[] base = solid(BASE);
        FaceMap map = cube();
        int previous = 0;
        for (int stage = 1; stage <= 4; stage++) {
            int count = snowPixels(CoatPainter.paint(base, map, 0.0F, stage / 4.0F, 7L), 0, 0, SIZE, SIZE);
            assertTrue(count > previous, "stage " + stage + " added no snow");
            previous = count;
        }
    }

    @Test
    void firstSnowStageStaysOnTopAndUpperEdges() {
        int[] painted = CoatPainter.paint(solid(BASE), cube(), 0.0F, 0.25F, 7L);
        assertTrue(snowPixels(painted, 0, 0, SIZE / 2, SIZE / 2) > 0);
        assertEquals(0, snowPixels(painted, 0, SIZE * 3 / 4, SIZE, SIZE));
    }

    @Test
    void fullSnowCoversTheWholeTopFace() {
        int[] painted = CoatPainter.paint(solid(BASE), cube(), 0.0F, 1.0F, 3L);
        for (int y = 0; y < SIZE / 2; y++) {
            for (int x = 0; x < SIZE / 2; x++) {
                assertTrue((painted[y * SIZE + x] & 0xFF) > 180, "top pixel " + x + "," + y + " is not snow");
            }
        }
    }

    @Test
    void transparentPixelsStayTransparent() {
        int[] base = solid(BASE);
        base[5] = 0x00123456;
        int[] painted = CoatPainter.paint(base, cube(), 1.0F, 1.0F, 9L);
        assertEquals(0x00123456, painted[5]);
        for (int i = 0; i < painted.length; i++) {
            if (i != 5) {
                assertEquals(0xFF, painted[i] >>> 24);
            }
        }
    }

    @Test
    void shelteredCubeGetsNoSnowCap() {
        List<CubeBox> boxes = List.of(new CubeBox(0, 0, 0, 8, 16, 8), new CubeBox(-4, -8, -4, 12, 0, 12));
        FaceMap map = FaceMap.build(SIZE, SIZE, List.of(
                FaceMapTest.top(0, 0, 0.5F, 0.5F, 0, 0),
                FaceMapTest.side(0, 0.5F, 1, 1, 0, 16, 0)), boxes);
        int[] painted = CoatPainter.paint(solid(BASE), map, 0.0F, 0.25F, 7L);
        assertEquals(0, snowPixels(painted, 0, 0, SIZE, SIZE));
    }

    @Test
    void sameSeedPaintsTheSameCoat() {
        int[] base = solid(BASE);
        FaceMap map = cube();
        assertArrayEquals(CoatPainter.paint(base, map, 0.5F, 0.75F, 42L), CoatPainter.paint(base, map, 0.5F, 0.75F, 42L));
    }

    @Test
    void worksWithoutModelShape() {
        int[] base = solid(BASE);
        int[] painted = CoatPainter.paint(base, FaceMap.empty(SIZE, SIZE), 1.0F, 1.0F, 1L);
        assertTrue(brightness(painted) < brightness(base));
    }

    @Test
    void rejectsMismatchedPixelCount() {
        assertThrows(IllegalArgumentException.class, () -> CoatPainter.paint(new int[3], cube(), 1.0F, 0.0F, 1L));
    }
}
