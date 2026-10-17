package org.lts.weathercoat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class Coats {
    public static final int SNOW_MASK_ORDER = 1000;
    private static final int UNTINTED = -1;

    private Coats() {
    }

    public static Identifier texture(Identifier texture, Model<?> model, Object state) {
        if (!(state instanceof CoatRenderState coat) || coat.weathercoat$stages() <= 0) {
            return texture;
        }
        return CoatTextures.get().resolve(texture, model, coat.weathercoat$wetStage(), coat.weathercoat$snowStage(),
                coat.weathercoat$stages());
    }

    public static boolean isTinted(int color) {
        return (color | 0xFF000000) != UNTINTED;
    }

    public static <S> void submitSnowMask(Identifier texture, Model<? super S> model, S state, PoseStack poseStack,
            SubmitNodeCollector collector, int light, int overlay, Function<Identifier, RenderType> renderType) {
        if (!(state instanceof CoatRenderState coat) || coat.weathercoat$snowStage() <= 0) {
            return;
        }
        Identifier mask = CoatTextures.get().resolveSnowMask(texture, model, coat.weathercoat$snowStage(), coat.weathercoat$stages());
        if (mask == null) {
            return;
        }
        collector.order(SNOW_MASK_ORDER).submitModel(model, state, poseStack, renderType.apply(mask), light, overlay, UNTINTED,
                null, EntityRenderState.NO_OUTLINE);
    }
}
