package org.lts.weathercoat.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lts.weathercoat.mixin.ModelPartAccessor;
import org.lts.weathercoat.paint.CubeBox;
import org.lts.weathercoat.paint.FaceQuad;
import org.lts.weathercoat.paint.ModelShape;

public final class ModelFaces {
    private static final int MAX_DEPTH = 32;

    private ModelFaces() {
    }

    public static ModelShape capture(Model<?> model) {
        if (model == null) {
            return ModelShape.EMPTY;
        }
        List<FaceQuad> faces = new ArrayList<>();
        List<CubeBox> cubes = new ArrayList<>();
        walk(model.root(), new Matrix4f(), faces, cubes, 0);
        return new ModelShape(faces, cubes);
    }

    private static void walk(ModelPart part, Matrix4f parent, List<FaceQuad> faces, List<CubeBox> cubes, int depth) {
        if (depth > MAX_DEPTH) {
            return;
        }
        PartPose pose = part.getInitialPose();
        Matrix4f matrix = new Matrix4f(parent).translate(pose.x(), pose.y(), pose.z());
        if (pose.xRot() != 0.0F || pose.yRot() != 0.0F || pose.zRot() != 0.0F) {
            matrix.rotate(new Quaternionf().rotationZYX(pose.zRot(), pose.yRot(), pose.xRot()));
        }
        if (pose.xScale() != 1.0F || pose.yScale() != 1.0F || pose.zScale() != 1.0F) {
            matrix.scale(pose.xScale(), pose.yScale(), pose.zScale());
        }
        ModelPartAccessor access = (ModelPartAccessor) (Object) part;
        Vector3f scratch = new Vector3f();
        for (ModelPart.Cube cube : access.weathercoat$cubes()) {
            int index = cubes.size();
            cubes.add(box(cube, matrix, scratch));
            for (ModelPart.Polygon polygon : cube.polygons) {
                FaceQuad quad = toQuad(polygon, matrix, scratch, index);
                if (quad != null) {
                    faces.add(quad);
                }
            }
        }
        for (ModelPart child : access.weathercoat$children().values()) {
            walk(child, matrix, faces, cubes, depth + 1);
        }
    }

    private static CubeBox box(ModelPart.Cube cube, Matrix4f matrix, Vector3f scratch) {
        float[] xs = new float[8];
        float[] ys = new float[8];
        float[] zs = new float[8];
        for (int i = 0; i < 8; i++) {
            matrix.transformPosition(scratch.set(
                    (i & 1) == 0 ? cube.minX : cube.maxX,
                    (i & 2) == 0 ? cube.minY : cube.maxY,
                    (i & 4) == 0 ? cube.minZ : cube.maxZ));
            xs[i] = scratch.x;
            ys[i] = scratch.y;
            zs[i] = scratch.z;
        }
        return CubeBox.around(xs, ys, zs);
    }

    private static FaceQuad toQuad(ModelPart.Polygon polygon, Matrix4f matrix, Vector3f scratch, int cube) {
        ModelPart.Vertex[] vertices = polygon.vertices();
        if (vertices.length < 3) {
            return null;
        }
        int count = vertices.length;
        float[] u = new float[count];
        float[] v = new float[count];
        float[] x = new float[count];
        float[] y = new float[count];
        float[] z = new float[count];
        for (int i = 0; i < count; i++) {
            ModelPart.Vertex vertex = vertices[i];
            matrix.transformPosition(scratch.set(vertex.x(), vertex.y(), vertex.z()));
            u[i] = vertex.u();
            v[i] = vertex.v();
            x[i] = scratch.x;
            y[i] = scratch.y;
            z[i] = scratch.z;
        }
        matrix.transformDirection(scratch.set(polygon.normal()));
        float length = scratch.length();
        if (length < 1.0E-6F) {
            return new FaceQuad(u, v, x, y, z, 0.0F, 0.0F, 0.0F, cube);
        }
        return new FaceQuad(u, v, x, y, z, scratch.x / length, scratch.y / length, scratch.z / length, cube);
    }
}
