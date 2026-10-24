package org.lts.weathercoat.track;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lts.weathercoat.WeatherCoatConfig;

public final class CoatTracker {
    private static final int UPDATE_INTERVAL = 4;
    private static final float SECONDS_PER_UPDATE = UPDATE_INTERVAL / 20.0F;
    private static final int FORGET_AFTER_TICKS = 200;
    private static final int HEAT_CHECK_TICKS = 20;
    private static final int HEAT_RADIUS = 2;
    private static final float POWDER_SNOW_STAGES_PER_SECOND = 2.0F;
    private static final double MOVING_BLOCKS_PER_UPDATE = 0.1;
    private static final double PARTICLE_DISTANCE_SQ = 32.0 * 32.0;
    private static final CoatTracker INSTANCE = new CoatTracker();

    private final Int2ObjectMap<CoatProgress> coats = new Int2ObjectOpenHashMap<>();
    private final Map<EntityType<?>, Boolean> affectedTypes = new IdentityHashMap<>();
    private final JoinCatchUp catchUp = new JoinCatchUp();
    private String typesSignature = "";
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
            catchUp.reset();
        }
        if (current == null || !config.enabled) {
            coats.clear();
            return;
        }
        String signature = Exclusions.signature(config);
        if (!signature.equals(typesSignature)) {
            typesSignature = signature;
            affectedTypes.clear();
            coats.clear();
        }
        tick++;
        rainSeconds = current.isRaining() ? rainSeconds + 1.0F / 20.0F : 0.0F;
        if (catchUp.tick(current.getRainLevel(1.0F)) && config.catchUpOnSight) {
            rainSeconds = Math.max(rainSeconds, (float) config.stages * Math.max(config.wetSecondsPerStage, config.snowSecondsPerStage));
            coats.clear();
        }
        Entity camera = minecraft.getCameraEntity();
        Vec3 eye = camera != null ? camera.position() : null;
        double maxDistanceSq = (double) config.maxDistance * config.maxDistance;
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        for (Entity entity : current.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !isAffected(living, config)) {
                continue;
            }
            double distanceSq = eye != null ? living.distanceToSqr(eye) : 0.0;
            if (distanceSq > maxDistanceSq) {
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
            boolean particles = config.particlesEnabled && distanceSq <= PARTICLE_DISTANCE_SQ
                    && !(firstPerson && living == camera);
            update(current, living, coat, config, fresh, particles);
        }
        if (tick % 100 == 0) {
            forgetStale();
        }
    }

    private void update(ClientLevel level, LivingEntity entity, CoatProgress coat, WeatherCoatConfig config, boolean fresh,
            boolean particles) {
        int stages = config.stages;
        RandomSource random = level.getRandom();
        coat.setFrostStage(config.frostEnabled ? Climate.frostStage(entity.getPercentFrozen(), stages) : 0);
        if (entity.isOnFire() || entity.isInLava()) {
            if (particles && !coat.isClean()) {
                burst(level, entity, random, ParticleTypes.WHITE_SMOKE, 6);
            }
            coat.clear();
            return;
        }
        if (entity instanceof Wolf wolf && wolf.getShakeAnim(1.0F) > 0.0F) {
            if (particles && coat.snowStage() > 0) {
                burst(level, entity, random, ParticleTypes.SNOWFLAKE, 8);
            }
            coat.clear();
            return;
        }
        BlockPos head = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        Biome.Precipitation precipitation = level.precipitationAt(head);
        boolean raining = config.rainEnabled && precipitation == Biome.Precipitation.RAIN;
        boolean snowing = config.snowEnabled && precipitation == Biome.Precipitation.SNOW;
        boolean powderSnow = config.snowEnabled && inPowderSnow(level, entity);
        if (fresh) {
            if (config.catchUpOnSight) {
                float wet = raining ? rainSeconds / config.wetSecondsPerStage : 0.0F;
                float snow = snowing ? rainSeconds / config.snowSecondsPerStage : 0.0F;
                coat.start(wet, snow, stages);
            }
            coat.moveSince(entity.getX(), entity.getZ());
            return;
        }
        boolean moving = coat.moveSince(entity.getX(), entity.getZ()) > MOVING_BLOCKS_PER_UPDATE;
        Holder<Biome> biome = level.getBiome(head);
        boolean climate = config.climateEnabled;
        if (climate && coat.heatCheckDue(tick, HEAT_CHECK_TICKS)) {
            coat.setNearHeat(nearHeat(level, entity), tick);
        }
        boolean nearHeat = climate && coat.nearHeat();
        boolean sunny = climate && level.isBrightOutside() && !level.isRaining() && level.canSeeSky(head);
        boolean humid = climate && isHumid(biome);
        float temperature = climate ? biome.value().getBaseTemperature() : 0.0F;
        float intensity = Math.max(0.5F, level.getRainLevel(1.0F)) * (level.isThundering() ? 1.5F : 1.0F);

        float wetDelta;
        if (!config.rainEnabled) {
            wetDelta = -stages;
        } else if (raining) {
            wetDelta = SECONDS_PER_UPDATE * intensity / config.wetSecondsPerStage;
        } else if (entity.isInWater()) {
            wetDelta = 0.0F;
        } else {
            wetDelta = -SECONDS_PER_UPDATE * Climate.dryFactor(temperature, humid, sunny, nearHeat) / config.drySecondsPerStage;
        }

        float snowDelta;
        if (!config.snowEnabled) {
            snowDelta = -stages;
        } else if (powderSnow) {
            snowDelta = SECONDS_PER_UPDATE * POWDER_SNOW_STAGES_PER_SECOND;
        } else if (snowing) {
            snowDelta = SECONDS_PER_UPDATE * intensity / config.snowSecondsPerStage;
        } else {
            boolean warm = !biome.value().coldEnoughToSnow(head, level.getSeaLevel());
            snowDelta = -SECONDS_PER_UPDATE * Climate.meltFactor(warm, temperature, sunny, nearHeat, moving)
                    / config.meltSecondsPerStage;
        }
        coat.advance(wetDelta, snowDelta, stages);

        if (particles) {
            boolean steaming = nearHeat && (wetDelta < 0.0F || snowDelta < 0.0F);
            spawnParticles(level, entity, coat, random, stages, raining, moving, steaming);
        }
    }

    private static void spawnParticles(ClientLevel level, LivingEntity entity, CoatProgress coat, RandomSource random, int stages,
            boolean raining, boolean moving, boolean steaming) {
        AABB box = entity.getBoundingBox();
        if (coat.wetStage() > 0 && !raining && random.nextFloat() < 0.35F * coat.wetStage() / stages) {
            double y = box.minY + box.getYsize() * (0.2 + 0.5 * random.nextDouble());
            level.addParticle(ParticleTypes.FALLING_WATER, randomX(box, random), y, randomZ(box, random), 0.0, 0.0, 0.0);
        }
        if (coat.snowStage() > 0 && moving && random.nextFloat() < 0.5F * coat.snowStage() / stages) {
            level.addParticle(ParticleTypes.SNOWFLAKE, randomX(box, random), box.maxY - 0.1, randomZ(box, random),
                    (random.nextDouble() - 0.5) * 0.05, -0.02, (random.nextDouble() - 0.5) * 0.05);
        }
        if (steaming && (coat.wetStage() > 0 || coat.snowStage() > 0) && random.nextFloat() < 0.6F) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, randomX(box, random), box.maxY, randomZ(box, random), 0.0, 0.03, 0.0);
        }
    }

    private static void burst(ClientLevel level, LivingEntity entity, RandomSource random, SimpleParticleType type, int count) {
        AABB box = entity.getBoundingBox();
        for (int i = 0; i < count; i++) {
            double y = box.minY + random.nextDouble() * box.getYsize();
            level.addParticle(type, randomX(box, random), y, randomZ(box, random),
                    (random.nextDouble() - 0.5) * 0.1, 0.05, (random.nextDouble() - 0.5) * 0.1);
        }
    }

    private static double randomX(AABB box, RandomSource random) {
        return box.minX + random.nextDouble() * box.getXsize();
    }

    private static double randomZ(AABB box, RandomSource random) {
        return box.minZ + random.nextDouble() * box.getZsize();
    }

    private static boolean inPowderSnow(ClientLevel level, LivingEntity entity) {
        return level.getBlockState(entity.blockPosition()).is(Blocks.POWDER_SNOW)
                || level.getBlockState(BlockPos.containing(entity.getEyePosition())).is(Blocks.POWDER_SNOW);
    }

    public static boolean isHumid(Holder<Biome> biome) {
        return biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP) || biome.is(BiomeTags.IS_JUNGLE);
    }

    private static boolean nearHeat(ClientLevel level, LivingEntity entity) {
        BlockPos feet = entity.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -HEAT_RADIUS; dx <= HEAT_RADIUS; dx++) {
                for (int dz = -HEAT_RADIUS; dz <= HEAT_RADIUS; dz++) {
                    cursor.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
                    if (isHeatSource(level.getBlockState(cursor))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isHeatSource(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.getFluidState().is(FluidTags.LAVA)) {
            return true;
        }
        if (state.is(BlockTags.CAMPFIRES)) {
            return CampfireBlock.isLitCampfire(state);
        }
        return state.getBlock() instanceof AbstractFurnaceBlock && state.getValue(AbstractFurnaceBlock.LIT);
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

    private boolean isAffected(LivingEntity entity, WeatherCoatConfig config) {
        if (entity instanceof Player) {
            return config.affectPlayers;
        }
        if (entity instanceof ArmorStand) {
            return false;
        }
        EntityType<?> type = entity.getType();
        Boolean known = affectedTypes.get(type);
        if (known == null) {
            known = Exclusions.isAffected(isAquatic(type.getCategory()), type.fireImmune(),
                    EntityType.getKey(type).toString(), config);
            affectedTypes.put(type, known);
        }
        return known;
    }

    private static boolean isAquatic(MobCategory category) {
        return category == MobCategory.WATER_CREATURE || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE || category == MobCategory.AXOLOTLS;
    }

    public CoatProgress coatOf(Entity entity) {
        return coats.get(entity.getId());
    }

    public void clear() {
        coats.clear();
        level = null;
    }
}
