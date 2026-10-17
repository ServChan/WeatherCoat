package org.lts.weathercoat.mixin;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.lts.weathercoat.render.CoatRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements CoatRenderState {
    @Unique
    private int weathercoat$wet;
    @Unique
    private int weathercoat$snow;
    @Unique
    private int weathercoat$frost;
    @Unique
    private int weathercoat$stageCount;

    @Override
    public int weathercoat$wetStage() {
        return weathercoat$wet;
    }

    @Override
    public int weathercoat$snowStage() {
        return weathercoat$snow;
    }

    @Override
    public int weathercoat$frostStage() {
        return weathercoat$frost;
    }

    @Override
    public int weathercoat$stages() {
        return weathercoat$stageCount;
    }

    @Override
    public void weathercoat$setCoat(int wetStage, int snowStage, int frostStage, int stages) {
        weathercoat$wet = wetStage;
        weathercoat$snow = snowStage;
        weathercoat$frost = frostStage;
        weathercoat$stageCount = stages;
    }
}
