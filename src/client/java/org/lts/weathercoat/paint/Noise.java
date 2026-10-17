package org.lts.weathercoat.paint;

public final class Noise {
    private Noise() {
    }

    public static float hash(int x, int y, long seed) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0xC2B2AE3D27D4EB4FL + y * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 40) / (float) (1L << 24);
    }

    public static float value(int x, int y, int cell, long seed) {
        int size = Math.max(1, cell);
        int cx = Math.floorDiv(x, size);
        int cy = Math.floorDiv(y, size);
        float fx = smooth((x - cx * size + 0.5F) / size);
        float fy = smooth((y - cy * size + 0.5F) / size);
        float a = hash(cx, cy, seed);
        float b = hash(cx + 1, cy, seed);
        float c = hash(cx, cy + 1, seed);
        float d = hash(cx + 1, cy + 1, seed);
        float top = a + (b - a) * fx;
        float bottom = c + (d - c) * fx;
        return top + (bottom - top) * fy;
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }
}
