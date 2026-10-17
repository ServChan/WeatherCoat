package org.lts.weathercoat.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.lts.weathercoat.render.CoatTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureManager.class)
public abstract class TextureManagerMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private void weathercoat$forgetCoats(PreparableReloadListener.SharedState sharedState, Executor taskExecutor,
            PreparableReloadListener.PreparationBarrier barrier, Executor reloadExecutor,
            CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        CoatTextures.get().requestInvalidate();
    }
}
