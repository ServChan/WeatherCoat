package org.lts.weathercoat.paint;

import java.util.Arrays;

public final class GroundPainter {
    public static final int TILE_BLOCKS = 8;
    public static final int PIXELS_PER_BLOCK = 16;
    public static final int SIZE = TILE_BLOCKS * PIXELS_PER_BLOCK;
    public static final int STAGES = 6;
    public static final int RIPPLE_FRAMES = 4;
    static final float PUDDLE_HALO = 0.06F;
    static final float SNOW_DUST = 0.05F;
    static final float RIM = 0.012F;
    private static final int RIPPLES_PER_TILE = 56;
    private static final int[] RIPPLE_ALPHA = {110, 78, 44};

    private GroundPainter() {
    }

    public static float[] puddleField(long seed) {
        float[] field = new float[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                field[y * SIZE + x] = 0.45F * Noise.tiled(x, y, 64, SIZE, seed)
                        + 0.30F * Noise.tiled(x, y, 32, SIZE, seed + 1)
                        + 0.17F * Noise.tiled(x, y, 16, SIZE, seed + 2)
                        + 0.08F * Noise.tiled(x, y, 4, SIZE, seed + 3);
            }
        }
        return rank(field);
    }

    public static float[] snowField(long seed) {
        float[] field = new float[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                field[y * SIZE + x] = 0.40F * Noise.tiled(x, y, 32, SIZE, seed)
                        + 0.30F * Noise.tiled(x, y, 8, SIZE, seed + 1)
                        + 0.30F * Noise.hash(x, y, seed + 2);
            }
        }
        return rank(field);
    }

    static float[] rank(float[] field) {
        long[] keys = new long[field.length];
        for (int i = 0; i < field.length; i++) {
            keys[i] = ((long) Float.floatToIntBits(Math.max(0.0F, field[i])) << 32) | i;
        }
        Arrays.sort(keys);
        float[] ranked = new float[field.length];
        float last = Math.max(1, field.length - 1);
        for (int order = 0; order < keys.length; order++) {
            ranked[(int) keys[order]] = order / last;
        }
        return ranked;
    }

    public static float[] blockPeaks(float[] field) {
        float[] peaks = new float[TILE_BLOCKS * TILE_BLOCKS];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int block = (y / PIXELS_PER_BLOCK) * TILE_BLOCKS + x / PIXELS_PER_BLOCK;
                peaks[block] = Math.max(peaks[block], field[y * SIZE + x]);
            }
        }
        return peaks;
    }

    public static int clampStage(int stage) {
        return Math.max(0, Math.min(STAGES, stage));
    }

    public static float puddleCoverage(int stage) {
        if (stage <= 0) {
            return 0.0F;
        }
        return 0.05F + 0.27F * (clampStage(stage) - 1) / (STAGES - 1);
    }

    public static float snowCoverage(int stage) {
        if (stage <= 0) {
            return 0.0F;
        }
        return 0.30F + 0.68F * (clampStage(stage) - 1) / (STAGES - 1);
    }

    public static float puddleCutoff(int stage) {
        return stage <= 0 ? 2.0F : 1.0F - puddleCoverage(stage) - PUDDLE_HALO;
    }

    public static float snowCutoff(int stage) {
        return stage <= 0 ? 2.0F : 1.0F - snowCoverage(stage) - SNOW_DUST;
    }

    public static int[] paintPuddles(float[] field, int stage, int frame, long seed) {
        int[] pixels = new int[SIZE * SIZE];
        if (stage <= 0) {
            return pixels;
        }
        float coverage = puddleCoverage(stage);
        float threshold = 1.0F - coverage;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float f = field[y * SIZE + x];
                if (f >= threshold) {
                    pixels[y * SIZE + x] = water(x, y, (f - threshold) / coverage, f < threshold + RIM, seed);
                } else if (f >= threshold - PUDDLE_HALO) {
                    float damp = 1.0F - (threshold - f) / PUDDLE_HALO;
                    pixels[y * SIZE + x] = argb(Math.round(56 * damp), 10, 14, 20);
                }
            }
        }
        if (frame > 0) {
            ripples(pixels, field, threshold, frame, seed);
        }
        return pixels;
    }

    private static int water(int x, int y, float depth, boolean rim, long seed) {
        if (rim) {
            return argb(72, 118, 138, 160);
        }
        float deep = (float) Math.pow(Math.min(1.0F, depth * 2.5F), 0.7);
        float r = lerp(84, 44, deep);
        float g = lerp(102, 60, deep);
        float b = lerp(124, 86, deep);
        float a = lerp(118, 168, deep);
        float sky = Noise.tiled(x + y / 2, y, 16, SIZE, seed + 7);
        if (sky > 0.68F) {
            float shine = Math.min(1.0F, (sky - 0.68F) / 0.2F) * 0.55F;
            r = lerp(r, 168, shine);
            g = lerp(g, 186, shine);
            b = lerp(b, 210, shine);
            a = lerp(a, 150, shine);
        }
        float grain = (Noise.hash(x, y, seed + 9) - 0.5F) * 8.0F;
        return argb(Math.round(a), Math.round(r + grain), Math.round(g + grain), Math.round(b + grain));
    }

    private static void ripples(int[] pixels, float[] field, float threshold, int frame, long seed) {
        for (int i = 0; i < RIPPLES_PER_TILE; i++) {
            int cx = (int) (Noise.hash(i, 0, seed + 11) * SIZE);
            int cy = (int) (Noise.hash(i, 1, seed + 11) * SIZE);
            int phase = (int) (Noise.hash(i, 2, seed + 11) * RIPPLE_FRAMES);
            int radius = Math.floorMod(frame - 1 + phase, RIPPLE_FRAMES);
            if (radius >= RIPPLE_ALPHA.length || field[cy * SIZE + cx] < threshold + RIM) {
                continue;
            }
            int alpha = RIPPLE_ALPHA[radius];
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (Math.abs(distance - radius) >= 0.5) {
                        continue;
                    }
                    int x = Math.floorMod(cx + dx, SIZE);
                    int y = Math.floorMod(cy + dy, SIZE);
                    if (field[y * SIZE + x] >= threshold + RIM) {
                        pixels[y * SIZE + x] = argb(alpha, 186, 200, 218);
                    }
                }
            }
        }
    }

    public static int[] paintSnow(float[] field, int stage, long seed) {
        int[] pixels = new int[SIZE * SIZE];
        if (stage <= 0) {
            return pixels;
        }
        float coverage = snowCoverage(stage);
        float threshold = 1.0F - coverage;
        float density = (float) (clampStage(stage) - 1) / (STAGES - 1);
        float maxAlpha = lerp(0.34F, 0.80F, density);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float f = field[y * SIZE + x];
                float sparkle = Noise.hash(x, y, seed + 5);
                if (f >= threshold) {
                    float amount = Math.min(1.0F, (f - threshold) / 0.18F);
                    float alpha = maxAlpha * (0.55F + 0.45F * amount);
                    if (sparkle < 0.035F) {
                        pixels[y * SIZE + x] = argb(Math.round(255 * Math.min(0.92F, alpha + 0.2F)), 255, 255, 255);
                    } else {
                        int shade = Math.round((Noise.hash(x, y, seed + 6) - 0.5F) * 10.0F);
                        pixels[y * SIZE + x] = argb(Math.round(255 * alpha), 234 + shade / 2, 241 + shade / 2, 250);
                    }
                } else if (f >= threshold - SNOW_DUST && sparkle < 0.3F) {
                    pixels[y * SIZE + x] = argb(Math.round(255 * maxAlpha * 0.4F), 238, 244, 252);
                }
            }
        }
        return pixels;
    }

    static int argb(int a, int r, int g, int b) {
        return clamp(a) << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
