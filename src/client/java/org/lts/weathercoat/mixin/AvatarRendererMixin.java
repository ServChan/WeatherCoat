package org.lts.weathercoat.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import org.lts.weathercoat.WeatherCoatConfig;
import org.lts.weathercoat.render.Coats;
import org.lts.weathercoat.track.CoatProgress;
import org.lts.weathercoat.track.CoatTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @ModifyArg(method = "renderHand", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityTranslucent(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private Identifier weathercoat$coatFirstPersonHand(Identifier skin) {
        WeatherCoatConfig config = WeatherCoatConfig.current();
        Minecraft minecraft = Minecraft.getInstance();
        if (!config.enabled || minecraft.player == null) {
            return skin;
        }
        CoatProgress coat = CoatTracker.get().coatOf(minecraft.player);
        if (coat == null) {
            return skin;
        }
        return Coats.texture(skin, ((LivingEntityRenderer<?, ?, ?>) (Object) this).getModel(), coat.wetStage(), coat.snowStage(),
                coat.frostStage(), config.stages);
    }
}
