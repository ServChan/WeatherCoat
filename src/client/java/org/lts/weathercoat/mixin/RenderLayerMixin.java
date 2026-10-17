package org.lts.weathercoat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.lts.weathercoat.render.Coats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderLayer.class)
public abstract class RenderLayerMixin {
    @ModifyVariable(method = "renderColoredCutoutModel", at = @At("HEAD"), argsOnly = true)
    private static <S extends LivingEntityRenderState> Identifier weathercoat$coatLayerTexture(Identifier texture, Model<? super S> model,
            Identifier original, PoseStack poseStack, SubmitNodeCollector collector, int light, S state, int color, int order) {
        if (Coats.isTinted(color)) {
            Coats.submitSnowMask(texture, model, state, poseStack, collector, light,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F), RenderTypes::entityCutout);
        }
        return Coats.texture(texture, model, state);
    }
}
