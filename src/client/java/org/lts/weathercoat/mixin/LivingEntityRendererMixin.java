package org.lts.weathercoat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.lts.weathercoat.WeatherCoatConfig;
import org.lts.weathercoat.render.CoatRenderState;
import org.lts.weathercoat.render.Coats;
import org.lts.weathercoat.track.CoatProgress;
import org.lts.weathercoat.track.CoatTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Shadow
    protected M model;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void weathercoat$extractCoat(T entity, S state, float partialTicks, CallbackInfo ci) {
        CoatRenderState coatState = (CoatRenderState) state;
        WeatherCoatConfig config = WeatherCoatConfig.current();
        CoatProgress coat = config.enabled ? CoatTracker.get().coatOf(entity) : null;
        if (coat == null) {
            coatState.weathercoat$setCoat(0, 0, 0);
            return;
        }
        coatState.weathercoat$setCoat(coat.wetStage(), coat.snowStage(), config.stages);
    }

    @ModifyExpressionValue(method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;"))
    private Identifier weathercoat$coatTexture(Identifier texture, S state) {
        return Coats.texture(texture, model, state);
    }
}
