package org.lts.weathercoat.paint;

public final class CoatPainter {
    private static final int DROPLET = 0xFFD2E2FF;
    private static final int SHEEN = 0xFFFFFFFF;
    private static final int SNOW_EDGE = 0xFFC9D5E6;
    private static final int FROST = 0xFFE4EAF4;

    private CoatPainter() {
    }

    public static int[] paint(int[] base, FaceMap map, float wet, float snow, long seed) {
        int width = map.width();
        int height = map.height();
        if (base.length != width * height) {
            throw new IllegalArgumentException("Pixel count " + base.length + " does not match " + width + "x" + height);
        }
        int[] out = base.clone();
        float wetLevel = clamp01(wet);
        float snowLevel = clamp01(snow);
        int block = Math.max(1, width / 64);
        if (wetLevel > 0.0F) {
            paintWet(out, map, wetLevel, seed, block);
        }
        if (snowLevel > 0.0F) {
            paintSnow(out, map, snowLevel, seed ^ 0x5DEECE66DL, block);
        }
        return out;
    }

    static void paintWet(int[] pixels, FaceMap map, float wet, long seed, int block) {
        int width = map.width();
        int height = map.height();
        float darken = 1.0F - 0.34F * wet;
        float saturate = 1.0F + 0.35F * wet;
        float sheenChance = 0.008F + 0.022F * wet;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int argb = pixels[index];
                int alpha = argb >>> 24;
                if (alpha == 0) {
                    continue;
                }
                float r = (argb >> 16) & 0xFF;
                float g = (argb >> 8) & 0xFF;
                float b = argb & 0xFF;
                float luma = 0.299F * r + 0.587F * g + 0.114F * b;
                r = (luma + (r - luma) * saturate) * darken;
                g = (luma + (g - luma) * saturate) * darken;
                b = (luma + (b - luma) * saturate) * darken + 7.0F * wet;
                int color = pack(alpha, r, g, b);
                byte kind = map.kind(x, y);
                int bx = x / block;
                int by = y / block;
                if (kind == FaceMap.SIDE) {
                    float streak = streakLength(bx, seed, wet);
                    if (streak > 0.0F && map.depth(x, y) <= streak) {
                        color = scale(color, 0.86F);
                        boolean tip = wet >= 0.5F && isStreakTip(map, x, y + block, streak);
                        if (tip) {
                            color = mix(color, DROPLET, 0.55F);
                        }
                    }
                }
                if (kind != FaceMap.NONE && kind != FaceMap.BOTTOM && Noise.hash(bx, by, seed) < sheenChance) {
                    color = mix(color, SHEEN, 0.1F + 0.2F * wet);
                }
                pixels[index] = color;
            }
        }
    }

    static float streakLength(int column, long seed, float wet) {
        float chance = 0.22F + 0.2F * wet;
        if (Noise.hash(column, 7919, seed) >= chance) {
            return 0.0F;
        }
        return wet * (0.35F + 0.65F * Noise.hash(column, 104729, seed));
    }

    private static boolean isStreakTip(FaceMap map, int x, int belowY, float streak) {
        if (belowY >= map.height() || map.kind(x, belowY) != FaceMap.SIDE) {
            return true;
        }
        return map.depth(x, belowY) > streak;
    }

    static void paintSnow(int[] pixels, FaceMap map, float snow, long seed, int block) {
        int width = map.width();
        int height = map.height();
        int cell = Math.max(2, width / 32);
        boolean[] covered = new boolean[pixels.length];
        float[] noise = new float[pixels.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if ((pixels[index] >>> 24) == 0) {
                    continue;
                }
                float n = Noise.value(x, y, cell, seed);
                noise[index] = n;
                covered[index] = isSnowCovered(map, x, y, n, snow, seed, block);
            }
        }
        float frost = snow > 0.5F ? (snow - 0.5F) * 0.3F : 0.0F;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int argb = pixels[index];
                int alpha = argb >>> 24;
                if (alpha == 0) {
                    continue;
                }
                if (covered[index]) {
                    int shade = 236 + Math.round(16.0F * noise[index]);
                    int snowColor = pack(255, shade - 6, shade - 2, shade + 3);
                    if (isEdge(covered, map, x, y)) {
                        snowColor = mix(snowColor, SNOW_EDGE, 0.7F);
                    }
                    pixels[index] = (alpha << 24) | (snowColor & 0xFFFFFF);
                } else if (frost > 0.0F && map.kind(x, y) != FaceMap.NONE) {
                    pixels[index] = mix(argb, FROST, frost);
                }
            }
        }
    }

    static boolean isSnowCovered(FaceMap map, int x, int y, float n, float snow, long seed, int block) {
        byte kind = map.kind(x, y);
        if (kind == FaceMap.TOP) {
            return map.open(x, y) && n < 0.1F + 0.95F * snow;
        }
        if (kind != FaceMap.SIDE) {
            return false;
        }
        float band = snow * 0.45F + (n - 0.5F) * 0.25F;
        if (map.open(x, y) && map.depth(x, y) < band) {
            return true;
        }
        return snow >= 0.5F && Noise.hash(x / block, y / block, seed) < 0.04F * snow;
    }

    private static boolean isEdge(boolean[] covered, FaceMap map, int x, int y) {
        int width = map.width();
        int[][] around = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] offset : around) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (map.kind(nx, ny) == FaceMap.NONE || map.kind(nx, ny) == FaceMap.BOTTOM) {
                continue;
            }
            if (!covered[ny * width + nx]) {
                return true;
            }
        }
        return false;
    }

    static int pack(int alpha, float r, float g, float b) {
        return (alpha << 24) | (channel(r) << 16) | (channel(g) << 8) | channel(b);
    }

    static int scale(int argb, float factor) {
        return pack(argb >>> 24, ((argb >> 16) & 0xFF) * factor, ((argb >> 8) & 0xFF) * factor, (argb & 0xFF) * factor);
    }

    static int mix(int argb, int target, float amount) {
        float keep = 1.0F - amount;
        float r = ((argb >> 16) & 0xFF) * keep + ((target >> 16) & 0xFF) * amount;
        float g = ((argb >> 8) & 0xFF) * keep + ((target >> 8) & 0xFF) * amount;
        float b = (argb & 0xFF) * keep + (target & 0xFF) * amount;
        return pack(argb >>> 24, r, g, b);
    }

    private static int channel(float value) {
        int rounded = Math.round(value);
        return rounded < 0 ? 0 : Math.min(255, rounded);
    }

    private static float clamp01(float value) {
        return value < 0.0F ? 0.0F : Math.min(1.0F, value);
    }
}
