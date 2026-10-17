package org.lts.weathercoat.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.resources.Identifier;
import org.lts.weathercoat.render.CoatRenderState;
import org.lts.weathercoat.render.CoatTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {
    private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"
            + "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
            + "Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

    @ModifyArg(method = RENDER_LAYERS, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private Identifier weathercoat$coatEquipment(Identifier texture, @Local(argsOnly = true) Model<?> model,
            @Local(argsOnly = true) Object state) {
        return weathercoat$resolve(texture, model, state);
    }

    @ModifyArg(method = RENDER_LAYERS, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCullGlint(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private Identifier weathercoat$coatEnchantedEquipment(Identifier texture, @Local(argsOnly = true) Model<?> model,
            @Local(argsOnly = true) Object state) {
        return weathercoat$resolve(texture, model, state);
    }

    @Unique
    private static Identifier weathercoat$resolve(Identifier texture, Model<?> model, Object state) {
        if (!(state instanceof CoatRenderState coat)) {
            return texture;
        }
        return CoatTextures.get().resolve(texture, model, coat.weathercoat$wetStage(), coat.weathercoat$snowStage(),
                coat.weathercoat$stages());
    }
}
