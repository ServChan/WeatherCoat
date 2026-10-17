package org.lts.weathercoat.paint;

import java.util.List;

public final class FaceMap {
    public static final byte NONE = 0;
    public static final byte BOTTOM = 1;
    public static final byte SIDE = 2;
    public static final byte TOP = 3;

    private static final float FACING_THRESHOLD = 0.5F;
    private static final float PROBE = 0.5F;

    private final int width;
    private final int height;
    private final byte[] kind;
    private final float[] depth;
    private final float[] level;
    private final boolean[] open;

    private FaceMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.kind = new byte[width * height];
        this.depth = new float[width * height];
        this.level = new float[width * height];
        this.open = new boolean[width * height];
    }

    public static FaceMap empty(int width, int height) {
        return new FaceMap(width, height);
    }

    public static FaceMap build(int width, int height, List<FaceQuad> quads, List<CubeBox> boxes) {
        FaceMap map = new FaceMap(width, height);
        if (quads.isEmpty()) {
            return map;
        }
        float modelTop = Float.MAX_VALUE;
        float modelBottom = -Float.MAX_VALUE;
        for (FaceQuad quad : quads) {
            modelTop = Math.min(modelTop, quad.top());
            modelBottom = Math.max(modelBottom, quad.bottom());
        }
        float modelRange = modelBottom - modelTop;
        for (FaceQuad quad : quads) {
            map.rasterize(quad, boxes, modelTop, modelRange);
        }
        return map;
    }

    private void rasterize(FaceQuad quad, List<CubeBox> boxes, float modelTop, float modelRange) {
        int x0 = Math.max(0, (int) Math.floor(quad.minU() * width + 1.0E-3F));
        int x1 = Math.min(width, (int) Math.ceil(quad.maxU() * width - 1.0E-3F));
        int y0 = Math.max(0, (int) Math.floor(quad.minV() * height + 1.0E-3F));
        int y1 = Math.min(height, (int) Math.ceil(quad.maxV() * height - 1.0E-3F));
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        byte faceKind = quad.normalY() < -FACING_THRESHOLD ? TOP : quad.normalY() > FACING_THRESHOLD ? BOTTOM : SIDE;
        float faceTop = quad.top();
        float faceRange = quad.bottom() - faceTop;
        CubeBox own = quad.cube() >= 0 && quad.cube() < boxes.size() ? boxes.get(quad.cube()) : null;
        float[] position = new float[3];
        for (int y = y0; y < y1; y++) {
            float atV = (y + 0.5F) / height;
            for (int x = x0; x < x1; x++) {
                int index = y * width + x;
                if (faceKind < kind[index]) {
                    continue;
                }
                quad.positionAt((x + 0.5F) / width, atV, position);
                float h = position[1];
                kind[index] = faceKind;
                depth[index] = faceRange > 0.01F ? clamp01((h - faceTop) / faceRange) : 0.0F;
                level[index] = modelRange > 0.01F ? clamp01((h - modelTop) / modelRange) : 0.0F;
                open[index] = switch (faceKind) {
                    case TOP -> !sheltered(position[0], h - PROBE, position[2], quad.cube(), own, boxes);
                    case SIDE -> !sheltered(position[0] - quad.normalX() * PROBE, faceTop - PROBE,
                            position[2] - quad.normalZ() * PROBE, quad.cube(), own, boxes);
                    default -> false;
                };
            }
        }
    }

    static boolean sheltered(float x, float y, float z, int cube, CubeBox own, List<CubeBox> boxes) {
        for (int i = 0; i < boxes.size(); i++) {
            if (i == cube) {
                continue;
            }
            CubeBox other = boxes.get(i);
            if (!other.contains(x, y, z)) {
                continue;
            }
            if (own != null && other.contains(own.centerX(), own.centerY(), own.centerZ())) {
                continue;
            }
            return true;
        }
        return false;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public byte kind(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return NONE;
        }
        return kind[y * width + x];
    }

    public float depth(int x, int y) {
        return depth[y * width + x];
    }

    public float level(int x, int y) {
        return level[y * width + x];
    }

    public boolean open(int x, int y) {
        return open[y * width + x];
    }

    public boolean mapped() {
        for (byte value : kind) {
            if (value != NONE) {
                return true;
            }
        }
        return false;
    }

    private static float clamp01(float value) {
        return value < 0.0F ? 0.0F : Math.min(1.0F, value);
    }
}
