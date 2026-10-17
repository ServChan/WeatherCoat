package org.lts.weathercoat.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
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
    private static final int EVICT_EVERY_TICKS = 200;
    private static final int MAX_PIXELS = 1024 * 1024;
    private static final int MASK_KEY = 2_000_000;
    private static final CoatTextures INSTANCE = new CoatTextures();

    private final Map<Model<?>, Map<Identifier, BaseEntry>> bases = new IdentityHashMap<>();
    private final Map<Model<?>, Map<Identifier, Integer>> shapeIds = new IdentityHashMap<>();
    private final List<BaseEntry> retired = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WeatherCoat-Painter");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });
    private int generation;
    private int nextShape;
    private int ticks;
    private volatile boolean invalidateRequested;

    private CoatTextures() {
    }

    public static CoatTextures get() {
        return INSTANCE;
    }

    public Identifier resolve(Identifier base, Model<?> model, int wetStage, int snowStage, int frostStage, int stages) {
        if (base == null || stages <= 0 || (wetStage <= 0 && snowStage <= 0 && frostStage <= 0)) {
            return base;
        }
        BaseEntry entry = entry(base, model);
        if (entry == null) {
            return base;
        }
        int wet = percent(wetStage, stages);
        int snow = percent(snowStage, stages);
        int frost = percent(frostStage, stages);
        Variant variant = variant(entry, wet, snow, frost, false);
        if (variant.ready) {
            return variant.id;
        }
        Variant fallback = entry.closestReady(wet, snow, frost);
        if (fallback == null) {
            return base;
        }
        fallback.lastUsed = System.currentTimeMillis();
        return fallback.id;
    }

    public Identifier resolveSnowMask(Identifier base, Model<?> model, int snowStage, int stages) {
        if (base == null || stages <= 0 || snowStage <= 0) {
            return null;
        }
        BaseEntry entry = entry(base, model);
        if (entry == null) {
            return null;
        }
        Variant mask = variant(entry, 0, percent(snowStage, stages), 0, true);
        return mask.ready ? mask.id : null;
    }

    private BaseEntry entry(Identifier base, Model<?> model) {
        if (WeatherCoatClient.MOD_ID.equals(base.getNamespace())) {
            return null;
        }
        AbstractTexture source = Minecraft.getInstance().getTextureManager().getTexture(base);
        Map<Identifier, BaseEntry> byTexture = bases.computeIfAbsent(model, ignored -> new HashMap<>());
        BaseEntry entry = byTexture.get(base);
        if (entry != null && entry.source != source) {
            retired.add(entry);
            entry = null;
        }
        if (entry == null) {
            entry = new BaseEntry(base, model, source, shapeId(base, model, source));
            byTexture.put(base, entry);
            startLoading(entry, model);
        }
        entry.lastUsed = System.currentTimeMillis();
        return entry.state == State.READY ? entry : null;
    }

    private int shapeId(Identifier base, Model<?> model, AbstractTexture source) {
        Map<Identifier, Integer> byTexture = shapeIds.computeIfAbsent(model, ignored -> new HashMap<>());
        Integer known = byTexture.get(base);
        boolean replaced = false;
        for (BaseEntry old : retired) {
            if (old.base.equals(base) && old.model == model && old.source != source) {
                replaced = true;
                break;
            }
        }
        if (known != null && !replaced) {
            return known;
        }
        int id = nextShape++;
        byTexture.put(base, id);
        return id;
    }

    private Variant variant(BaseEntry entry, int wet, int snow, int frost, boolean mask) {
        int key = (mask ? MASK_KEY : 0) + (wet * 101 + snow) * 101 + frost;
        Variant variant = entry.variants.get(key);
        if (variant == null) {
            variant = new Variant(variantId(entry, wet, snow, frost, mask), key, wet, snow, frost, mask);
            entry.variants.put(key, variant);
            startPainting(entry, variant);
        }
        variant.lastUsed = System.currentTimeMillis();
        return variant;
    }

    private static int percent(int stage, int stages) {
        return Math.round(100.0F * Math.max(0, Math.min(stage, stages)) / stages);
    }

    private void startLoading(BaseEntry entry, Model<?> model) {
        ModelShape faces;
        try {
            faces = ModelFaces.capture(model);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not read the model shape for {}; painting without it", entry.base, exception);
            faces = ModelShape.EMPTY;
        }
        int[] dynamicPixels = null;
        int dynamicWidth = 0;
        int dynamicHeight = 0;
        if (entry.source instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
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
        submit(entry, () -> {
            Pixels pixels = copied != null ? new Pixels(copiedWidth, copiedHeight, copied) : readResource(entry.base);
            FaceMap map = pixels == null ? null : shape.map(pixels.width, pixels.height);
            Minecraft.getInstance().execute(() -> {
                if (!isCurrent(entry, expected)) {
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
        }, () -> entry.state = State.FAILED);
    }

    private void startPainting(BaseEntry entry, Variant variant) {
        Pixels pixels = entry.pixels;
        FaceMap map = entry.faces;
        int expected = generation;
        long seed = entry.base.toString().hashCode() * 0x9E3779B9L;
        submit(entry, () -> {
            int[] painted = variant.mask
                    ? CoatPainter.snowMask(pixels.argb, map, variant.snow / 100.0F, seed)
                    : CoatPainter.paint(pixels.argb, map, variant.wet / 100.0F, variant.snow / 100.0F, variant.frost / 100.0F, seed);
            Minecraft.getInstance().execute(() -> upload(entry, variant, pixels, painted, expected));
        }, () -> entry.variants.remove(variant.key, variant));
    }

    private boolean isCurrent(BaseEntry entry, int expected) {
        if (expected != generation) {
            return false;
        }
        Map<Identifier, BaseEntry> byTexture = bases.get(entry.model);
        return byTexture != null && byTexture.get(entry.base) == entry;
    }

    private void upload(BaseEntry entry, Variant variant, Pixels pixels, int[] painted, int expected) {
        if (!isCurrent(entry, expected) || entry.variants.get(variant.key) != variant) {
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
            entry.variants.remove(variant.key);
        }
    }

    private void submit(BaseEntry entry, Runnable job, Runnable onFailure) {
        try {
            worker.execute(() -> {
                try {
                    job.run();
                } catch (RuntimeException | OutOfMemoryError exception) {
                    LOGGER.warn("Could not paint the weather coat for {}", entry.base, exception);
                    Minecraft.getInstance().execute(onFailure);
                }
            });
        } catch (RejectedExecutionException exception) {
            onFailure.run();
        }
    }

    private static Pixels readResource(Identifier id) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
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

    private static Identifier variantId(BaseEntry entry, int wet, int snow, int frost, boolean mask) {
        String path = entry.base.getPath();
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        String leaf = mask ? String.format(Locale.ROOT, "snow%d", snow) : String.format(Locale.ROOT, "w%d_s%d_f%d", wet, snow, frost);
        return Identifier.fromNamespaceAndPath(WeatherCoatClient.MOD_ID,
                String.format(Locale.ROOT, "coat/%s/%s/m%d/%s", entry.base.getNamespace(), path, entry.shape, leaf));
    }

    public void tick() {
        if (invalidateRequested) {
            invalidateNow();
            return;
        }
        for (BaseEntry entry : retired) {
            releaseAll(entry);
        }
        retired.clear();
        if (++ticks < EVICT_EVERY_TICKS) {
            return;
        }
        ticks = 0;
        long now = System.currentTimeMillis();
        Iterator<Map<Identifier, BaseEntry>> models = bases.values().iterator();
        while (models.hasNext()) {
            Map<Identifier, BaseEntry> byTexture = models.next();
            Iterator<BaseEntry> entries = byTexture.values().iterator();
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
            if (byTexture.isEmpty()) {
                models.remove();
            }
        }
    }

    public void requestInvalidate() {
        invalidateRequested = true;
    }

    public void clear() {
        if (!bases.isEmpty() || !retired.isEmpty() || invalidateRequested) {
            invalidateNow();
        }
    }

    private void invalidateNow() {
        invalidateRequested = false;
        generation++;
        for (Map<Identifier, BaseEntry> byTexture : bases.values()) {
            for (BaseEntry entry : byTexture.values()) {
                releaseAll(entry);
            }
        }
        for (BaseEntry entry : retired) {
            releaseAll(entry);
        }
        retired.clear();
        bases.clear();
        shapeIds.clear();
        nextShape = 0;
        ticks = 0;
    }

    private static void releaseAll(BaseEntry entry) {
        for (Variant variant : entry.variants.values()) {
            release(variant);
        }
        entry.variants.clear();
    }

    private static void release(Variant variant) {
        if (variant.ready) {
            Minecraft.getInstance().getTextureManager().release(variant.id);
            variant.ready = false;
        }
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

    private static final class BaseEntry {
        final Identifier base;
        final Model<?> model;
        final AbstractTexture source;
        final int shape;
        final Map<Integer, Variant> variants = new HashMap<>();
        State state = State.LOADING;
        Pixels pixels;
        FaceMap faces;
        long lastUsed;

        BaseEntry(Identifier base, Model<?> model, AbstractTexture source, int shape) {
            this.base = base;
            this.model = model;
            this.source = source;
            this.shape = shape;
        }

        Variant closestReady(int wet, int snow, int frost) {
            Variant best = null;
            int bestDistance = Integer.MAX_VALUE;
            for (Variant variant : variants.values()) {
                if (!variant.ready || variant.mask) {
                    continue;
                }
                int distance = Math.abs(variant.wet - wet) + Math.abs(variant.snow - snow) + Math.abs(variant.frost - frost);
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
        final int key;
        final int wet;
        final int snow;
        final int frost;
        final boolean mask;
        boolean ready;
        long lastUsed;

        Variant(Identifier id, int key, int wet, int snow, int frost, boolean mask) {
            this.id = id;
            this.key = key;
            this.wet = wet;
            this.snow = snow;
            this.frost = frost;
            this.mask = mask;
        }
    }
}
