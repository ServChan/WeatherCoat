package org.lts.weathercoat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.resources.Identifier;
import org.lts.weathercoat.render.Coats;
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

    @Unique
    private Identifier weathercoat$layerTexture;

    @ModifyArg(method = RENDER_LAYERS, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private Identifier weathercoat$coatEquipment(Identifier texture, @Local(argsOnly = true) Model<?> model,
            @Local(argsOnly = true) Object state) {
        weathercoat$layerTexture = texture;
        return Coats.texture(texture, model, state);
    }

    @ModifyArg(method = RENDER_LAYERS, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCullGlint(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private Identifier weathercoat$coatEnchantedEquipment(Identifier texture, @Local(argsOnly = true) Model<?> model,
            @Local(argsOnly = true) Object state) {
        weathercoat$layerTexture = texture;
        return Coats.texture(texture, model, state);
    }

    @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"))
    private void weathercoat$snowOverDyedEquipment(OrderedSubmitNodeCollector ordered, Model<Object> model, Object state,
            PoseStack poseStack, RenderType renderType, int light, int overlay, int color, UvMapping mapping, int outline,
            Operation<Void> original, @Local(argsOnly = true) SubmitNodeCollector collector) {
        original.call(ordered, model, state, poseStack, renderType, light, overlay, color, mapping, outline);
        Identifier texture = weathercoat$layerTexture;
        weathercoat$layerTexture = null;
        if (texture != null && Coats.isTinted(color)) {
            Coats.submitSnowMask(texture, model, state, poseStack, collector, light, OverlayTexture.NO_OVERLAY,
                    RenderTypes::armorCutoutNoCull);
        }
    }
}
