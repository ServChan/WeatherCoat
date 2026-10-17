package org.lts.weathercoat.paint;

public record CubeBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    public static CubeBox around(float[] xs, float[] ys, float[] zs) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            minX = Math.min(minX, xs[i]);
            minY = Math.min(minY, ys[i]);
            minZ = Math.min(minZ, zs[i]);
            maxX = Math.max(maxX, xs[i]);
            maxY = Math.max(maxY, ys[i]);
            maxZ = Math.max(maxZ, zs[i]);
        }
        return new CubeBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean contains(float x, float y, float z) {
        return x > minX && x < maxX && y > minY && y < maxY && z > minZ && z < maxZ;
    }

    public float centerX() {
        return (minX + maxX) * 0.5F;
    }

    public float centerY() {
        return (minY + maxY) * 0.5F;
    }

    public float centerZ() {
        return (minZ + maxZ) * 0.5F;
    }
}
