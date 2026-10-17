package org.lts.weathercoat.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FaceMapTest {
    static FaceQuad top(float u0, float v0, float u1, float v1, float y) {
        return top(u0, v0, u1, v1, y, -1);
    }

    static FaceQuad top(float u0, float v0, float u1, float v1, float y, int cube) {
        return new FaceQuad(new float[]{u0, u1, u1, u0}, new float[]{v0, v0, v1, v1},
                new float[]{0, 8, 8, 0}, new float[]{y, y, y, y}, new float[]{0, 0, 8, 8}, 0.0F, -1.0F, 0.0F, cube);
    }

    static FaceQuad side(float u0, float v0, float u1, float v1, float topY, float bottomY) {
        return side(u0, v0, u1, v1, topY, bottomY, -1);
    }

    static FaceQuad side(float u0, float v0, float u1, float v1, float topY, float bottomY, int cube) {
        return new FaceQuad(new float[]{u0, u1, u1, u0}, new float[]{v0, v0, v1, v1},
                new float[]{0, 8, 8, 0}, new float[]{topY, topY, bottomY, bottomY}, new float[]{0, 0, 0, 0},
                0.0F, 0.0F, -1.0F, cube);
    }

    static FaceMap build(int width, int height, List<FaceQuad> quads) {
        return FaceMap.build(width, height, quads, List.of());
    }

    @Test
    void classifiesFacesByNormal() {
        FaceQuad bottom = new FaceQuad(new float[]{0.5F, 0.75F, 0.75F, 0.5F}, new float[]{0, 0, 0.25F, 0.25F},
                new float[]{0, 8, 8, 0}, new float[]{8, 8, 8, 8}, new float[]{0, 0, 8, 8}, 0.0F, 1.0F, 0.0F, -1);
        FaceMap map = build(16, 16, List.of(top(0, 0, 0.25F, 0.25F, 0), side(0, 0.5F, 0.5F, 1.0F, 0, 8), bottom));
        assertEquals(FaceMap.TOP, map.kind(1, 1));
        assertEquals(FaceMap.SIDE, map.kind(2, 12));
        assertEquals(FaceMap.BOTTOM, map.kind(9, 1));
        assertEquals(FaceMap.NONE, map.kind(15, 15));
        assertTrue(map.mapped());
    }

    @Test
    void sideDepthRunsFromTopEdgeToBottomEdge() {
        FaceMap map = build(16, 16, List.of(side(0, 0, 1, 1, 0, 16)));
        assertTrue(map.depth(4, 0) < 0.1F);
        assertTrue(map.depth(4, 15) > 0.9F);
        assertTrue(map.depth(4, 7) < map.depth(4, 8));
    }

    @Test
    void flippedSideUvStillMeasuresWorldHeight() {
        FaceQuad upsideDown = new FaceQuad(new float[]{0, 1, 1, 0}, new float[]{0, 0, 1, 1},
                new float[]{0, 8, 8, 0}, new float[]{16, 16, 0, 0}, new float[]{0, 0, 0, 0}, 0.0F, 0.0F, -1.0F, -1);
        FaceMap map = build(16, 16, List.of(upsideDown));
        assertTrue(map.depth(4, 15) < 0.1F);
        assertTrue(map.depth(4, 0) > 0.9F);
    }

    @Test
    void topFaceWinsOverSideOnSharedPixels() {
        FaceMap map = build(8, 8, List.of(top(0, 0, 1, 1, 0), side(0, 0, 1, 1, 0, 4)));
        assertEquals(FaceMap.TOP, map.kind(3, 3));
        map = build(8, 8, List.of(side(0, 0, 1, 1, 0, 4), top(0, 0, 1, 1, 0)));
        assertEquals(FaceMap.TOP, map.kind(3, 3));
    }

    @Test
    void levelIsMeasuredAcrossTheWholeModel() {
        FaceMap map = build(16, 16, List.of(top(0, 0, 0.5F, 0.5F, 0), top(0.5F, 0.5F, 1, 1, 20)));
        assertEquals(0.0F, map.level(2, 2));
        assertEquals(1.0F, map.level(12, 12));
    }

    @Test
    void emptyShapeLeavesEverythingUnmapped() {
        assertFalse(build(8, 8, List.of()).mapped());
    }

    @Test
    void outOfRangeUvIsIgnored() {
        assertFalse(build(8, 8, List.of(top(1.2F, 1.2F, 1.5F, 1.5F, 0))).mapped());
    }

    @Test
    void topCoveredByAnotherCubeIsSheltered() {
        List<CubeBox> boxes = List.of(new CubeBox(0, 0, 0, 8, 8, 8), new CubeBox(-2, -6, -2, 10, 0, 10));
        FaceMap map = FaceMap.build(16, 16, List.of(top(0, 0, 0.5F, 0.5F, 0, 0), side(0, 0.5F, 1, 1, 0, 8, 0)), boxes);
        assertFalse(map.open(2, 2));
        assertFalse(map.open(4, 12));
    }

    @Test
    void openTopStaysOpen() {
        List<CubeBox> boxes = List.of(new CubeBox(0, 0, 0, 8, 8, 8), new CubeBox(20, -6, 20, 30, 0, 30));
        FaceMap map = FaceMap.build(16, 16, List.of(top(0, 0, 0.5F, 0.5F, 0, 0), side(0, 0.5F, 1, 1, 0, 8, 0)), boxes);
        assertTrue(map.open(2, 2));
        assertTrue(map.open(4, 12));
    }

    @Test
    void overlayLayerDoesNotShelterItsOwnCube() {
        List<CubeBox> boxes = List.of(new CubeBox(0, 0, 0, 8, 8, 8), new CubeBox(-1, -1, -1, 9, 9, 9));
        FaceMap map = FaceMap.build(16, 16, List.of(top(0, 0, 0.5F, 0.5F, 0, 0)), boxes);
        assertTrue(map.open(2, 2));
    }
}
