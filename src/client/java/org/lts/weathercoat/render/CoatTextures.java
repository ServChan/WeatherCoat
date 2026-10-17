package org.lts.weathercoat.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.lts.weathercoat.ModLog;
import org.lts.weathercoat.WeatherCoatClient;
import org.lts.weathercoat.paint.CoatPainter;
import org.lts.weathercoat.paint.FaceMap;
import org.lts.weathercoat.paint.ModelShape;

public final class CoatTextures {
    private static final ModLog LOGGER = ModLog.of("Textures");
    private static final long VARIANT_TTL_MS = 60_000L;
    private static final long BASE_TTL_MS = 180_000L;
    private static final int MAX_PIXELS = 1024 * 1024;
    private static final CoatTextures INSTANCE = new CoatTextures();

    private final Map<BaseKey, BaseEntry> bases = new HashMap<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WeatherCoat-Painter");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });
    private int generation;
    private int nextShape;
    private volatile boolean invalidateRequested;

    private CoatTextures() {
    }

    public static CoatTextures get() {
        return INSTANCE;
    }

    public Identifier resolve(Identifier base, Model<?> model, int wetStage, int snowStage, int stages) {
        if (base == null || stages <= 0 || (wetStage <= 0 && snowStage <= 0)) {
            return base;
        }
        if (WeatherCoatClient.MOD_ID.equals(base.getNamespace())) {
            return base;
        }
        if (invalidateRequested) {
            invalidateNow();
        }
        long now = System.currentTimeMillis();
        BaseKey baseKey = new BaseKey(base, model);
        BaseEntry entry = bases.get(baseKey);
        if (entry == null) {
            entry = new BaseEntry(baseKey, nextShape++);
            bases.put(baseKey, entry);
            startLoading(entry, model);
        }
        entry.lastUsed = now;
        if (entry.state != State.READY) {
            return base;
        }
        int wet = Math.round(100.0F * Math.min(wetStage, stages) / stages);
        int snow = Math.round(100.0F * Math.min(snowStage, stages) / stages);
        int key = wet * 1000 + snow;
        Variant variant = entry.variants.get(key);
        if (variant == null) {
            variant = new Variant(variantId(base, entry.shape, wet, snow), wet, snow);
            entry.variants.put(key, variant);
            startPainting(entry, variant);
        }
        variant.lastUsed = now;
        if (variant.ready) {
            return variant.id;
        }
        Variant fallback = entry.closestReady(wet, snow);
        if (fallback == null) {
            return base;
        }
        fallback.lastUsed = now;
        return fallback.id;
    }

    private void startLoading(BaseEntry entry, Model<?> model) {
        ModelShape faces;
        try {
            faces = ModelFaces.capture(model);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not read the model shape for {}; painting without it", entry.base, exception);
            faces = ModelShape.EMPTY;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int[] dynamicPixels = null;
        int dynamicWidth = 0;
        int dynamicHeight = 0;
        AbstractTexture texture = minecraft.getTextureManager().getTexture(entry.base);
        if (texture instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
            NativeImage image = dynamic.getPixels();
            dynamicWidth = image.getWidth();
            dynamicHeight = image.getHeight();
            if ((long) dynamicWidth * dynamicHeight <= MAX_PIXELS) {
                dynamicPixels = image.getPixels();
            }
        }
        int expected = generation;
        ModelShape shape = faces;
        int[] copied = dynamicPixels;
        int copiedWidth = dynamicWidth;
        int copiedHeight = dynamicHeight;
        submit(() -> {
            Pixels pixels = copied != null ? new Pixels(copiedWidth, copiedHeight, copied) : readResource(entry.base);
            FaceMap map = pixels == null ? null : shape.map(pixels.width, pixels.height);
            minecraft.execute(() -> {
                if (expected != generation || bases.get(entry.key) != entry) {
                    return;
                }
                if (pixels == null) {
                    entry.state = State.FAILED;
                    return;
                }
                entry.pixels = pixels;
                entry.faces = map;
                entry.state = State.READY;
            });
        }, entry);
    }

    private void startPainting(BaseEntry entry, Variant variant) {
        Pixels pixels = entry.pixels;
        FaceMap map = entry.faces;
        int expected = generation;
        long seed = entry.base.toString().hashCode() * 0x9E3779B9L;
        submit(() -> {
            int[] painted = CoatPainter.paint(pixels.argb, map, variant.wet / 100.0F, variant.snow / 100.0F, seed);
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> upload(entry, variant, pixels, painted, expected));
        }, entry);
    }

    private void upload(BaseEntry entry, Variant variant, Pixels pixels, int[] painted, int expected) {
        if (expected != generation || bases.get(entry.key) != entry || entry.variants.get(variant.key()) != variant) {
            return;
        }
        NativeImage image = new NativeImage(pixels.width, pixels.height, false);
        for (int y = 0; y < pixels.height; y++) {
            int row = y * pixels.width;
            for (int x = 0; x < pixels.width; x++) {
                image.setPixel(x, y, painted[row + x]);
            }
        }
        try {
            DynamicTexture texture = new DynamicTexture(variant.id::toString, image);
            Minecraft.getInstance().getTextureManager().register(variant.id, texture);
            variant.ready = true;
        } catch (RuntimeException exception) {
            image.close();
            LOGGER.warn("Could not upload {}", variant.id, exception);
            entry.variants.remove(variant.key());
        }
    }

    private void submit(Runnable job, BaseEntry entry) {
        try {
            worker.execute(() -> {
                try {
                    job.run();
                } catch (RuntimeException exception) {
                    LOGGER.warn("Could not paint the weather coat for {}", entry.base, exception);
                }
            });
        } catch (RejectedExecutionException exception) {
            entry.state = State.FAILED;
        }
    }

    private static Pixels readResource(Identifier id) {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            LOGGER.debug("No texture file for {}; the coat stays off for it", id);
            return null;
        }
        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
            int width = image.getWidth();
            int height = image.getHeight();
            if ((long) width * height > MAX_PIXELS) {
                LOGGER.debug("Texture {} is {}x{}; too large to paint", id, width, height);
                return null;
            }
            return new Pixels(width, height, image.getPixels());
        } catch (IOException exception) {
            LOGGER.warn("Could not read {}", id, exception);
            return null;
        }
    }

    private static Identifier variantId(Identifier base, int shape, int wet, int snow) {
        String path = base.getPath();
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return Identifier.fromNamespaceAndPath(WeatherCoatClient.MOD_ID,
                String.format(Locale.ROOT, "coat/%s/%s/m%d/w%d_s%d", base.getNamespace(), path, shape, wet, snow));
    }

    public void tick() {
        if (invalidateRequested) {
            invalidateNow();
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<BaseEntry> entries = bases.values().iterator();
        while (entries.hasNext()) {
            BaseEntry entry = entries.next();
            Iterator<Variant> variants = entry.variants.values().iterator();
            while (variants.hasNext()) {
                Variant variant = variants.next();
                if (now - variant.lastUsed > VARIANT_TTL_MS) {
                    release(variant);
                    variants.remove();
                }
            }
            if (entry.variants.isEmpty() && now - entry.lastUsed > BASE_TTL_MS) {
                entries.remove();
            }
        }
    }

    public void requestInvalidate() {
        invalidateRequested = true;
    }

    public void clear() {
        invalidateNow();
    }

    private void invalidateNow() {
        invalidateRequested = false;
        generation++;
        for (BaseEntry entry : bases.values()) {
            for (Variant variant : entry.variants.values()) {
                release(variant);
            }
        }
        bases.clear();
        nextShape = 0;
    }

    private static void release(Variant variant) {
        if (variant.ready) {
            Minecraft.getInstance().getTextureManager().release(variant.id);
            variant.ready = false;
        }
    }

    public int textureCount() {
        int count = 0;
        for (BaseEntry entry : bases.values()) {
            for (Variant variant : entry.variants.values()) {
                if (variant.ready) {
                    count++;
                }
            }
        }
        return count;
    }

    public void shutdown() {
        worker.shutdownNow();
    }

    private enum State {
        LOADING,
        READY,
        FAILED
    }

    private record Pixels(int width, int height, int[] argb) {
    }

    private record BaseKey(Identifier texture, Model<?> model) {
    }

    private static final class BaseEntry {
        final BaseKey key;
        final Identifier base;
        final int shape;
        final Map<Integer, Variant> variants = new HashMap<>();
        State state = State.LOADING;
        Pixels pixels;
        FaceMap faces;
        long lastUsed;

        BaseEntry(BaseKey key, int shape) {
            this.key = key;
            this.base = key.texture();
            this.shape = shape;
        }

        Variant closestReady(int wet, int snow) {
            Variant best = null;
            int bestDistance = Integer.MAX_VALUE;
            for (Variant variant : variants.values()) {
                if (!variant.ready) {
                    continue;
                }
                int distance = Math.abs(variant.wet - wet) + Math.abs(variant.snow - snow);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = variant;
                }
            }
            return best;
        }
    }

    private static final class Variant {
        final Identifier id;
        final int wet;
        final int snow;
        boolean ready;
        long lastUsed;

        Variant(Identifier id, int wet, int snow) {
            this.id = id;
            this.wet = wet;
            this.snow = snow;
        }

        int key() {
            return wet * 1000 + snow;
        }
    }
}
