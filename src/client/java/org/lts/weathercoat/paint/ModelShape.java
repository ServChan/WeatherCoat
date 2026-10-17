package org.lts.weathercoat.paint;

import java.util.List;

public record ModelShape(List<FaceQuad> faces, List<CubeBox> cubes) {
    public static final ModelShape EMPTY = new ModelShape(List.of(), List.of());

    public FaceMap map(int width, int height) {
        return FaceMap.build(width, height, faces, cubes);
    }
}
