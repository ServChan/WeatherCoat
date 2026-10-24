package org.lts.weathercoat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import org.lts.weathercoat.ground.GroundLayer;
import org.lts.weathercoat.render.CoatTextures;
import org.lts.weathercoat.track.CoatTracker;

public final class WeatherCoatClient implements ClientModInitializer {
    public static final String MOD_ID = "weathercoat";
    public static final ModLog LOGGER = ModLog.root();

    @Override
    public void onInitializeClient() {
        WeatherCoatConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CoatTracker.get().tick(client);
            GroundLayer.get().tick(client);
            if (client.level == null) {
                CoatTextures.get().clear();
            } else {
                CoatTextures.get().tick();
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CoatTracker.get().clear();
            GroundLayer.get().clear();
            CoatTextures.get().shutdown();
        });
        LevelExtractionEvents.END_EXTRACTION.register(context -> GroundLayer.get().extracted(context.camera().position()));
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> GroundLayer.get().submit(context.poseStack(), context.submitNodeCollector()));
        WeatherCoatConfig config = WeatherCoatConfig.current();
        LOGGER.ready("{} stages, rain {}, snow {}, puddles {}, ground snow {}", config.stages, config.rainEnabled, config.snowEnabled,
                config.puddlesEnabled, config.groundSnowEnabled);
    }
}
