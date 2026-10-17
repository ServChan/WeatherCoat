package org.lts.weathercoat.track;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.lts.weathercoat.WeatherCoatConfig;

public final class CoatTracker {
    private static final int UPDATE_INTERVAL = 4;
    private static final float SECONDS_PER_UPDATE = UPDATE_INTERVAL / 20.0F;
    private static final int FORGET_AFTER_TICKS = 200;
    private static final float WARM_MELT_FACTOR = 2.0F;
    private static final CoatTracker INSTANCE = new CoatTracker();

    private final Int2ObjectMap<CoatProgress> coats = new Int2ObjectOpenHashMap<>();
    private ClientLevel level;
    private int tick;
    private float rainSeconds;

    private CoatTracker() {
    }

    public static CoatTracker get() {
        return INSTANCE;
    }

    public void tick(Minecraft minecraft) {
        ClientLevel current = minecraft.level;
        WeatherCoatConfig config = WeatherCoatConfig.current();
        if (current != level) {
            level = current;
            coats.clear();
            rainSeconds = 0.0F;
        }
        if (current == null || !config.enabled) {
            coats.clear();
            return;
        }
        tick++;
        rainSeconds = current.isRaining() ? rainSeconds + 1.0F / 20.0F : 0.0F;
        Entity camera = minecraft.getCameraEntity();
        Vec3 eye = camera != null ? camera.position() : null;
        double maxDistanceSq = (double) config.maxDistance * config.maxDistance;
        int stages = config.stages;
        for (Entity entity : current.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !isAffected(living, config)) {
                continue;
            }
            if (eye != null && living.distanceToSqr(eye) > maxDistanceSq) {
                continue;
            }
            int id = living.getId();
            CoatProgress coat = coats.get(id);
            boolean fresh = coat == null;
            if (fresh) {
                coat = new CoatProgress();
                coats.put(id, coat);
            }
            coat.seen(tick);
            if (!fresh && (tick + id) % UPDATE_INTERVAL != 0) {
                continue;
            }
            update(current, living, coat, config, stages, fresh);
        }
        if (tick % 100 == 0) {
            forgetStale();
        }
    }

    private void update(ClientLevel level, LivingEntity entity, CoatProgress coat, WeatherCoatConfig config, int stages, boolean fresh) {
        if (entity.isOnFire() || entity.isInLava()) {
            coat.clear();
            return;
        }
        BlockPos head = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        Biome.Precipitation precipitation = level.precipitationAt(head);
        boolean raining = config.rainEnabled && precipitation == Biome.Precipitation.RAIN;
        boolean snowing = config.snowEnabled && precipitation == Biome.Precipitation.SNOW;
        if (fresh) {
            if (config.catchUpOnSight) {
                float wet = raining ? rainSeconds / config.wetSecondsPerStage : 0.0F;
                float snow = snowing ? rainSeconds / config.snowSecondsPerStage : 0.0F;
                coat.start(wet, snow, stages);
            }
            return;
        }
        float intensity = Math.max(0.5F, level.getRainLevel(1.0F)) * (level.isThundering() ? 1.5F : 1.0F);
        float wetDelta;
        if (!config.rainEnabled) {
            wetDelta = -stages;
        } else if (raining) {
            wetDelta = SECONDS_PER_UPDATE * intensity / config.wetSecondsPerStage;
        } else if (entity.isInWater()) {
            wetDelta = 0.0F;
        } else {
            wetDelta = -SECONDS_PER_UPDATE / config.drySecondsPerStage;
        }
        float snowDelta;
        if (!config.snowEnabled) {
            snowDelta = -stages;
        } else if (snowing) {
            snowDelta = SECONDS_PER_UPDATE * intensity / config.snowSecondsPerStage;
        } else {
            boolean warm = !level.getBiome(head).value().coldEnoughToSnow(head, level.getSeaLevel());
            snowDelta = -SECONDS_PER_UPDATE * (warm ? WARM_MELT_FACTOR : 1.0F) / config.meltSecondsPerStage;
        }
        coat.advance(wetDelta, snowDelta, stages);
    }

    private void forgetStale() {
        Iterator<Int2ObjectMap.Entry<CoatProgress>> iterator = coats.int2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            CoatProgress coat = iterator.next().getValue();
            if (tick - coat.lastSeenTick() > FORGET_AFTER_TICKS) {
                iterator.remove();
            }
        }
    }

    private static boolean isAffected(LivingEntity entity, WeatherCoatConfig config) {
        if (entity instanceof ArmorStand) {
            return false;
        }
        return config.affectPlayers || !(entity instanceof Player);
    }

    public CoatProgress coatOf(Entity entity) {
        return coats.get(entity.getId());
    }

    public int trackedCount() {
        return coats.size();
    }

    public void clear() {
        coats.clear();
        level = null;
    }
}
