package org.lts.weathercoat.render;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.lts.weathercoat.ModLog;

public final class TextureFeatures {
    private static final ModLog LOGGER = ModLog.of("ETF");
    private static final String MOD_ID = "entity_texture_features";
    private static final String UTILS = "traben.entity_texture_features.utils.ETFUtils2";
    private static final String METHOD = "getETFVariantNotNullForInjector";
    private static MethodHandle variant = find();

    private TextureFeatures() {
    }

    public static Identifier variantOf(Identifier texture) {
        MethodHandle handle = variant;
        if (handle == null || texture == null) {
            return texture;
        }
        try {
            Identifier chosen = (Identifier) handle.invokeExact(texture);
            return chosen != null ? chosen : texture;
        } catch (Throwable throwable) {
            variant = null;
            LOGGER.warn("Could not ask Entity Texture Features for the variant of {}; random textures stay dry", texture, throwable);
            return texture;
        }
    }

    private static MethodHandle find() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> utils = Class.forName(UTILS);
            MethodHandle handle = MethodHandles.publicLookup().findStatic(utils, METHOD,
                    MethodType.methodType(Identifier.class, Identifier.class));
            LOGGER.info("Using Entity Texture Features variants for weather coats");
            return handle;
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Entity Texture Features is loaded but {} was not found; random textures stay dry", METHOD, exception);
            return null;
        }
    }
}
