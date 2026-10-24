package org.lts.weathercoat.ground;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lts.weathercoat.WeatherCoatConfig;
import org.lts.weathercoat.paint.GroundPainter;
import org.lts.weathercoat.track.Climate;
import org.lts.weathercoat.track.CoatTracker;
import org.lts.weathercoat.track.GroundWeather;
import org.lts.weathercoat.track.JoinCatchUp;

public final class GroundLayer {
    private static final float SECONDS_PER_TICK = 1.0F / 20.0F;
    private static final int PASS_TICKS = 20;
    private static final int RIPPLE_TICKS = 3;
    private static final float RAIN_THRESHOLD = 0.2F;
    private static final float PUDDLE_LIFT = 0.004F;
    private static final float SNOW_LIFT = 0.007F;
    private static final int EDGE_STEPS = 4;
    private static final float EDGE_FADE = 0.45F;
    private static final float STEP_TOLERANCE = 0.01F;
    private static final int FLOATS_PER_VERTEX = 6;
    private static final int FLOATS_PER_QUAD = FLOATS_PER_VERTEX * 4;
    private static final byte NONE = 0;
    private static final byte RAIN = 1;
    private static final byte SNOW = 2;
    private static final GroundLayer INSTANCE = new GroundLayer();

    private final GroundWeather weather = new GroundWeather();
    private final JoinCatchUp catchUp = new JoinCatchUp();
    private final GroundTextures textures = new GroundTextures();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
    private ClientLevel level;
    private Scan building;
    private Scan ready;
    private boolean readyChanged;
    private Mesh puddleMesh;
    private Mesh snowMesh;
    private int puddleStage;
    private int snowStage;
    private int frame;
    private volatile Vec3 camera = Vec3.ZERO;

    private GroundLayer() {
    }

    public static GroundLayer get() {
        return INSTANCE;
    }

    public void tick(Minecraft minecraft) {
        ClientLevel current = minecraft.level;
        if (current != level) {
            level = current;
            weather.reset();
            catchUp.reset();
            forgetSurface();
        }
        if (current == null) {
            return;
        }
        WeatherCoatConfig config = WeatherCoatConfig.current();
        boolean puddlesOn = config.enabled && config.puddlesEnabled;
        boolean snowOn = config.enabled && config.groundSnowEnabled;
        float rain = current.getRainLevel(1.0F);
        boolean raining = rain > RAIN_THRESHOLD;
        if (catchUp.tick(rain) && config.catchUpOnSight) {
            weather.fill(puddlesOn, snowOn);
        }
        float intensity = raining ? Math.max(0.5F, rain) * (current.isThundering() ? 1.5F : 1.0F) : 0.0F;
        Entity viewer = minecraft.getCameraEntity();
        float dryFactor = 1.0F;
        float meltFactor = 1.0F;
        if (config.climateEnabled && viewer != null) {
            BlockPos at = viewer.blockPosition();
            Holder<Biome> biome = current.getBiome(at);
            float temperature = biome.value().getBaseTemperature();
            boolean sunny = current.isBrightOutside() && !raining;
            boolean warm = !biome.value().coldEnoughToSnow(at, current.getSeaLevel());
            dryFactor = Climate.dryFactor(temperature, CoatTracker.isHumid(biome), sunny, false);
            meltFactor = Climate.meltFactor(warm, temperature, sunny, false, false);
        }
        weather.advance(SECONDS_PER_TICK, intensity, dryFactor, meltFactor, puddlesOn, snowOn);
        int nextPuddles = weather.puddleStage();
        int nextSnow = weather.snowStage();
        if (viewer == null || (nextPuddles == 0 && nextSnow == 0 && (!raining || !(puddlesOn || snowOn)))) {
            puddleStage = 0;
            snowStage = 0;
            forgetSurface();
            return;
        }
        scanStep(current, viewer.blockPosition(), config.groundRadius);
        frame = raining ? 1 + (int) ((current.getGameTime() / RIPPLE_TICKS) % GroundPainter.RIPPLE_FRAMES) : 0;
        if (readyChanged || nextPuddles != puddleStage || nextSnow != snowStage) {
            readyChanged = false;
            puddleStage = nextPuddles;
            snowStage = nextSnow;
            rebuildMeshes();
        }
        if (puddleStage > 0) {
            for (int i = 0; i <= GroundPainter.RIPPLE_FRAMES; i++) {
                textures.puddle(puddleStage, i);
            }
        }
        textures.snow(snowStage);
    }

    public void clear() {
        level = null;
        weather.reset();
        forgetSurface();
    }

    private void forgetSurface() {
        building = null;
        ready = null;
        readyChanged = false;
        puddleMesh = null;
        snowMesh = null;
    }

    public void extracted(Vec3 cameraPosition) {
        camera = cameraPosition;
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
        Vec3 view = camera;
        submit(poseStack, collector, view, puddleMesh, textures.puddle(puddleStage, frame));
        submit(poseStack, collector, view, snowMesh, textures.snow(snowStage));
    }

    private static void submit(PoseStack poseStack, SubmitNodeCollector collector, Vec3 view, Mesh mesh, Identifier texture) {
        if (mesh == null || mesh.quads == 0 || texture == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(mesh.anchorX - view.x, mesh.anchorY - view.y, mesh.anchorZ - view.z);
        collector.submitCustomGeometry(poseStack, RenderTypes.text(texture), mesh::emit);
        poseStack.popPose();
    }

    private void scanStep(ClientLevel level, BlockPos centre, int radius) {
        if (building == null) {
            building = new Scan(centre, radius);
        }
        int rows = Math.max(1, (building.side + PASS_TICKS - 1) / PASS_TICKS);
        int seaLevel = level.getSeaLevel();
        for (int i = 0; i < rows && building.nextRow < building.side; i++, building.nextRow++) {
            int z = building.centreZ - building.radius + building.nextRow;
            for (int column = 0; column < building.side; column++) {
                scanColumn(level, building, building.nextRow * building.side + column,
                        building.centreX - building.radius + column, z, seaLevel);
            }
        }
        if (building.nextRow >= building.side) {
            ready = building;
            building = null;
            readyChanged = true;
        }
    }

    private void scanColumn(ClientLevel level, Scan scan, int index, int x, int z, int seaLevel) {
        scan.kind[index] = NONE;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        if (y < level.getMinY()) {
            scan.surface[index] = Float.NEGATIVE_INFINITY;
            return;
        }
        scan.surface[index] = y + 1.0F;
        cursor.set(x, y, z);
        BlockState state = level.getBlockState(cursor);
        if (!state.getFluidState().isEmpty()) {
            scan.surface[index] = y + state.getFluidState().getOwnHeight();
            return;
        }
        if (state.isAir() || isSnow(state)) {
            return;
        }
        float top = topOf(level, state, cursor);
        if (!(top > 0.0F)) {
            return;
        }
        scan.surface[index] = y + top;
        above.set(x, y + 1, z);
        BlockState over = level.getBlockState(above);
        if (!over.getFluidState().isEmpty() || isSnow(over)) {
            return;
        }
        Biome.Precipitation precipitation = level.getBiome(above).value().getPrecipitationAt(above, seaLevel);
        if (precipitation == Biome.Precipitation.RAIN && !state.is(BlockTags.LEAVES)) {
            scan.kind[index] = RAIN;
        } else if (precipitation == Biome.Precipitation.SNOW) {
            scan.kind[index] = SNOW;
        } else {
            return;
        }
        scan.y[index] = y;
        scan.top[index] = top;
        scan.light[index] = LightCoordsUtil.getLightCoords(level, top >= 1.0F ? above : cursor);
    }

    private static boolean isSnow(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW);
    }

    private static float topOf(ClientLevel level, BlockState state, BlockPos pos) {
        if (state.isSolidRender()) {
            return 1.0F;
        }
        VoxelShape shape = state.getShape(level, pos);
        if (shape.isEmpty()
                || shape.min(Direction.Axis.X) > 0.001 || shape.max(Direction.Axis.X) < 0.999
                || shape.min(Direction.Axis.Z) > 0.001 || shape.max(Direction.Axis.Z) < 0.999) {
            return -1.0F;
        }
        List<AABB> boxes = shape.toAabbs();
        return boxes.size() == 1 ? (float) boxes.getFirst().maxY : -1.0F;
    }

    private void rebuildMeshes() {
        Scan scan = ready;
        if (scan == null) {
            puddleMesh = null;
            snowMesh = null;
            return;
        }
        puddleMesh = puddleStage > 0 ? build(scan, RAIN, GroundPainter.puddleCutoff(puddleStage), PUDDLE_LIFT, true) : null;
        snowMesh = snowStage > 0 ? build(scan, SNOW, GroundPainter.snowCutoff(snowStage), SNOW_LIFT, false) : null;
    }

    private Mesh build(Scan scan, byte kind, float cutoff, float lift, boolean fadeAtDrops) {
        int count = 0;
        for (int index = 0; index < scan.kind.length; index++) {
            if (included(scan, index, kind, cutoff)) {
                count += fadeAtDrops && openSides(scan, index) != 0 ? EDGE_STEPS * EDGE_STEPS : 1;
            }
        }
        float[] data = new float[count * FLOATS_PER_QUAD];
        int[] lights = new int[count];
        int quad = 0;
        for (int index = 0; index < scan.kind.length && quad < count; index++) {
            if (!included(scan, index, kind, cutoff)) {
                continue;
            }
            int x = scan.worldX(index);
            int z = scan.worldZ(index);
            float localY = scan.y[index] - scan.centreY + scan.top[index] + lift;
            int open = fadeAtDrops ? openSides(scan, index) : 0;
            int steps = open == 0 ? 1 : EDGE_STEPS;
            for (int row = 0; row < steps; row++) {
                for (int column = 0; column < steps; column++) {
                    float x0 = (float) column / steps;
                    float z0 = (float) row / steps;
                    float x1 = (float) (column + 1) / steps;
                    float z1 = (float) (row + 1) / steps;
                    int base = quad * FLOATS_PER_QUAD;
                    corner(data, base, scan, x, z, localY, x0, z0, open);
                    corner(data, base + FLOATS_PER_VERTEX, scan, x, z, localY, x0, z1, open);
                    corner(data, base + FLOATS_PER_VERTEX * 2, scan, x, z, localY, x1, z1, open);
                    corner(data, base + FLOATS_PER_VERTEX * 3, scan, x, z, localY, x1, z0, open);
                    lights[quad] = scan.light[index];
                    quad++;
                }
            }
        }
        return new Mesh(scan.centreX, scan.centreY, scan.centreZ, data, lights, quad);
    }

    private static void corner(float[] data, int base, Scan scan, int x, int z, float localY, float inX, float inZ, int open) {
        float tile = GroundPainter.TILE_BLOCKS;
        data[base] = x - scan.centreX + inX;
        data[base + 1] = localY;
        data[base + 2] = z - scan.centreZ + inZ;
        data[base + 3] = (Math.floorMod(x, GroundPainter.TILE_BLOCKS) + inX) / tile;
        data[base + 4] = (Math.floorMod(z, GroundPainter.TILE_BLOCKS) + inZ) / tile;
        data[base + 5] = edgeAlpha(open, inX, inZ);
    }

    static float edgeAlpha(int open, float inX, float inZ) {
        float alpha = 1.0F;
        if ((open & 1) != 0) {
            alpha = Math.min(alpha, inX / EDGE_FADE);
        }
        if ((open & 2) != 0) {
            alpha = Math.min(alpha, (1.0F - inX) / EDGE_FADE);
        }
        if ((open & 4) != 0) {
            alpha = Math.min(alpha, inZ / EDGE_FADE);
        }
        if ((open & 8) != 0) {
            alpha = Math.min(alpha, (1.0F - inZ) / EDGE_FADE);
        }
        return Math.max(0.0F, Math.min(1.0F, alpha));
    }

    private static int openSides(Scan scan, int index) {
        int column = index % scan.side;
        int row = index / scan.side;
        float height = scan.surface[index];
        int open = 0;
        if (column > 0 && drops(scan, index - 1, height)) {
            open |= 1;
        }
        if (column < scan.side - 1 && drops(scan, index + 1, height)) {
            open |= 2;
        }
        if (row > 0 && drops(scan, index - scan.side, height)) {
            open |= 4;
        }
        if (row < scan.side - 1 && drops(scan, index + scan.side, height)) {
            open |= 8;
        }
        return open;
    }

    private static boolean drops(Scan scan, int neighbour, float height) {
        float other = scan.surface[neighbour];
        return other < height - STEP_TOLERANCE || (scan.kind[neighbour] != RAIN && other <= height + STEP_TOLERANCE);
    }

    private boolean included(Scan scan, int index, byte kind, float cutoff) {
        if (scan.kind[index] != kind) {
            return false;
        }
        int x = scan.worldX(index);
        int z = scan.worldZ(index);
        float peak = kind == RAIN ? textures.puddlePeak(x, z) : textures.snowPeak(x, z);
        return peak >= cutoff;
    }

    private static final class Scan {
        final int centreX;
        final int centreY;
        final int centreZ;
        final int radius;
        final int side;
        final byte[] kind;
        final int[] y;
        final float[] top;
        final float[] surface;
        final int[] light;
        int nextRow;

        Scan(BlockPos centre, int radius) {
            this.centreX = centre.getX();
            this.centreY = centre.getY();
            this.centreZ = centre.getZ();
            this.radius = radius;
            this.side = radius * 2 + 1;
            int cells = side * side;
            this.kind = new byte[cells];
            this.y = new int[cells];
            this.top = new float[cells];
            this.surface = new float[cells];
            this.light = new int[cells];
        }

        int worldX(int index) {
            return centreX - radius + index % side;
        }

        int worldZ(int index) {
            return centreZ - radius + index / side;
        }
    }

    private record Mesh(int anchorX, int anchorY, int anchorZ, float[] data, int[] lights, int quads) {
        void emit(PoseStack.Pose pose, VertexConsumer buffer) {
            for (int quad = 0; quad < quads; quad++) {
                int light = lights[quad];
                for (int corner = 0; corner < 4; corner++) {
                    int base = quad * FLOATS_PER_QUAD + corner * FLOATS_PER_VERTEX;
                    int alpha = Math.round(data[base + 5] * 255.0F);
                    buffer.addVertex(pose, data[base], data[base + 1], data[base + 2])
                            .setColor(alpha << 24 | 0xFFFFFF)
                            .setUv(data[base + 3], data[base + 4])
                            .setLight(light);
                }
            }
        }
    }
}
