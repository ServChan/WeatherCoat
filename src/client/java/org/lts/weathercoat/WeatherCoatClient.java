package org.lts.weathercoat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
            if (client.level == null) {
                CoatTextures.get().clear();
            } else {
                CoatTextures.get().tick();
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CoatTracker.get().clear();
            CoatTextures.get().shutdown();
        });
        WeatherCoatConfig config = WeatherCoatConfig.current();
        LOGGER.ready("{} stages, rain {}, snow {}", config.stages, config.rainEnabled, config.snowEnabled);
    }
}
