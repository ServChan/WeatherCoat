package org.lts.weathercoat.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.WolfCollarLayer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;
import org.lts.weathercoat.render.Coats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WolfCollarLayer.class)
public abstract class WolfCollarLayerMixin {
    @ModifyArg(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/WolfRenderState;FF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityCutout(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private Identifier weathercoat$coatCollar(Identifier texture, @Local(argsOnly = true) WolfRenderState state) {
        return Coats.texture(texture, ((RenderLayer<?, ?>) (Object) this).getParentModel(), state);
    }
}
