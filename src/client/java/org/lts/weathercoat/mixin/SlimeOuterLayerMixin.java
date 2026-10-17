package org.lts.weathercoat.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import org.lts.weathercoat.render.Coats;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SlimeOuterLayer.class)
public abstract class SlimeOuterLayerMixin {
    @Shadow
    @Final
    private SlimeModel model;

    @ModifyArg(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/SlimeRenderState;FF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityTranslucent(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private Identifier weathercoat$coatOuterSlime(Identifier texture, @Local(argsOnly = true) SlimeRenderState state) {
        return Coats.texture(texture, model, state);
    }
}
