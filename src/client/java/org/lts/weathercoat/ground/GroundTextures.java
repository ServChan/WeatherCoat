package org.lts.weathercoat.ground;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lts.weathercoat.ModLog;
import org.lts.weathercoat.WeatherCoatClient;
import org.lts.weathercoat.paint.GroundPainter;

final class GroundTextures {
    private static final ModLog LOGGER = ModLog.of("Ground");
    private static final long PUDDLE_SEED = 0x5EED9A11L;
    private static final long SNOW_SEED = 0x51A0F1A4EL;

    private float[] puddleField;
    private float[] puddlePeaks;
    private float[] snowField;
    private float[] snowPeaks;
    private final Identifier[][] puddles = new Identifier[GroundPainter.STAGES + 1][GroundPainter.RIPPLE_FRAMES + 1];
    private final Identifier[] snow = new Identifier[GroundPainter.STAGES + 1];
    private boolean failed;

    float puddlePeak(int x, int z) {
        if (puddlePeaks == null) {
            puddleField = GroundPainter.puddleField(PUDDLE_SEED);
            puddlePeaks = GroundPainter.blockPeaks(puddleField);
        }
        return puddlePeaks[cell(x, z)];
    }

    float snowPeak(int x, int z) {
        if (snowPeaks == null) {
            snowField = GroundPainter.snowField(SNOW_SEED);
            snowPeaks = GroundPainter.blockPeaks(snowField);
        }
        return snowPeaks[cell(x, z)];
    }

    private static int cell(int x, int z) {
        return Math.floorMod(z, GroundPainter.TILE_BLOCKS) * GroundPainter.TILE_BLOCKS + Math.floorMod(x, GroundPainter.TILE_BLOCKS);
    }

    Identifier puddle(int stage, int frame) {
        if (stage <= 0 || failed) {
            return null;
        }
        Identifier id = puddles[stage][frame];
        if (id == null) {
            puddlePeak(0, 0);
            id = upload("ground/puddle_" + stage + "_" + frame, GroundPainter.paintPuddles(puddleField, stage, frame, PUDDLE_SEED));
            puddles[stage][frame] = id;
        }
        return id;
    }

    Identifier snow(int stage) {
        if (stage <= 0 || failed) {
            return null;
        }
        Identifier id = snow[stage];
        if (id == null) {
            snowPeak(0, 0);
            id = upload("ground/snow_" + stage, GroundPainter.paintSnow(snowField, stage, SNOW_SEED));
            snow[stage] = id;
        }
        return id;
    }

    private Identifier upload(String path, int[] pixels) {
        Identifier id = Identifier.fromNamespaceAndPath(WeatherCoatClient.MOD_ID, path);
        int size = GroundPainter.SIZE;
        NativeImage image = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setPixel(x, y, pixels[y * size + x]);
            }
        }
        try {
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
            return id;
        } catch (RuntimeException exception) {
            image.close();
            failed = true;
            LOGGER.warn("Could not upload {}, ground effects are off", id, exception);
            return null;
        }
    }
}
