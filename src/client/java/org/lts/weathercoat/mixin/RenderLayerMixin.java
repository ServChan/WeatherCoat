package org.lts.weathercoat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.lts.weathercoat.render.CoatRenderState;
import org.lts.weathercoat.render.CoatTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderLayer.class)
public abstract class RenderLayerMixin {
    @ModifyVariable(method = "renderColoredCutoutModel", at = @At("HEAD"), argsOnly = true)
    private static Identifier weathercoat$coatLayerTexture(Identifier texture, Model<?> model, Identifier original,
            PoseStack poseStack, SubmitNodeCollector collector, int light, LivingEntityRenderState state, int color, int order) {
        CoatRenderState coatState = (CoatRenderState) state;
        return CoatTextures.get().resolve(texture, model,
                coatState.weathercoat$wetStage(), coatState.weathercoat$snowStage(), coatState.weathercoat$stages());
    }
}
