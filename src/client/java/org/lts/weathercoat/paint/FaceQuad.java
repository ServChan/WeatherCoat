package org.lts.weathercoat.paint;

public record FaceQuad(float[] u, float[] v, float[] x, float[] y, float[] z, float normalX, float normalY, float normalZ, int cube) {
    public FaceQuad {
        int count = u.length;
        if (count < 3 || v.length != count || x.length != count || y.length != count || z.length != count) {
            throw new IllegalArgumentException("A face needs at least three vertices with matching coordinates");
        }
    }

    public float minU() {
        return min(u);
    }

    public float maxU() {
        return max(u);
    }

    public float minV() {
        return min(v);
    }

    public float maxV() {
        return max(v);
    }

    public float top() {
        return min(y);
    }

    public float bottom() {
        return max(y);
    }

    public void positionAt(float atU, float atV, float[] out) {
        float u0 = minU();
        float u1 = maxU();
        float v0 = minV();
        float v1 = maxV();
        float fu = u1 - u0 > 1.0E-6F ? clamp01((atU - u0) / (u1 - u0)) : 0.0F;
        float fv = v1 - v0 > 1.0E-6F ? clamp01((atV - v0) / (v1 - v0)) : 0.0F;
        int c00 = nearest(u0, v0);
        int c10 = nearest(u1, v0);
        int c01 = nearest(u0, v1);
        int c11 = nearest(u1, v1);
        out[0] = bilinear(x, c00, c10, c01, c11, fu, fv);
        out[1] = bilinear(y, c00, c10, c01, c11, fu, fv);
        out[2] = bilinear(z, c00, c10, c01, c11, fu, fv);
    }

    private static float bilinear(float[] values, int c00, int c10, int c01, int c11, float fu, float fv) {
        float upper = values[c00] + (values[c10] - values[c00]) * fu;
        float lower = values[c01] + (values[c11] - values[c01]) * fu;
        return upper + (lower - upper) * fv;
    }

    private int nearest(float atU, float atV) {
        int best = 0;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < u.length; i++) {
            float du = u[i] - atU;
            float dv = v[i] - atV;
            float distance = du * du + dv * dv;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private static float min(float[] values) {
        float result = Float.MAX_VALUE;
        for (float value : values) {
            result = Math.min(result, value);
        }
        return result;
    }

    private static float max(float[] values) {
        float result = -Float.MAX_VALUE;
        for (float value : values) {
            result = Math.max(result, value);
        }
        return result;
    }

    private static float clamp01(float value) {
        return value < 0.0F ? 0.0F : Math.min(1.0F, value);
    }
}
