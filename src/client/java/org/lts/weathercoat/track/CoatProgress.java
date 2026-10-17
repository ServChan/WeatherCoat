package org.lts.weathercoat.track;

public final class CoatProgress {
    private float wet;
    private float snow;
    private int wetStage;
    private int snowStage;
    private int frostStage;
    private int lastSeenTick;
    private boolean nearHeat;
    private boolean heatChecked;
    private int heatCheckedTick;
    private boolean hasLastPosition;
    private double lastX;
    private double lastZ;

    public void advance(float wetDelta, float snowDelta, int stages) {
        wet = clamp(wet + wetDelta, stages);
        snow = clamp(snow + snowDelta, stages);
        wetStage = step(wetStage, wet, stages);
        snowStage = step(snowStage, snow, stages);
    }

    public void start(float wetValue, float snowValue, int stages) {
        wet = clamp(wetValue, stages);
        snow = clamp(snowValue, stages);
        wetStage = (int) Math.floor(wet);
        snowStage = (int) Math.floor(snow);
    }

    public void clear() {
        wet = 0.0F;
        snow = 0.0F;
        wetStage = 0;
        snowStage = 0;
    }

    public void setFrostStage(int stage) {
        frostStage = Math.max(0, stage);
    }

    public int frostStage() {
        return frostStage;
    }

    public boolean nearHeat() {
        return nearHeat;
    }

    public boolean heatCheckDue(int tick, int interval) {
        return !heatChecked || tick - heatCheckedTick >= interval;
    }

    public void setNearHeat(boolean value, int tick) {
        nearHeat = value;
        heatCheckedTick = tick;
        heatChecked = true;
    }

    public double moveSince(double x, double z) {
        double distance = hasLastPosition ? Math.hypot(x - lastX, z - lastZ) : 0.0;
        lastX = x;
        lastZ = z;
        hasLastPosition = true;
        return distance;
    }

    static int step(int current, float progress, int stages) {
        int stage = Math.min(current, stages);
        if (progress >= stage + 1) {
            stage = (int) Math.floor(progress);
        } else if (progress <= stage - 1) {
            stage = (int) Math.ceil(progress);
        }
        return Math.max(0, Math.min(stages, stage));
    }

    private static float clamp(float value, int stages) {
        if (!Float.isFinite(value) || value < 0.0F) {
            return 0.0F;
        }
        return Math.min(stages, value);
    }

    public float wet() {
        return wet;
    }

    public float snow() {
        return snow;
    }

    public int wetStage() {
        return wetStage;
    }

    public int snowStage() {
        return snowStage;
    }

    public boolean isClean() {
        return wet == 0.0F && snow == 0.0F && wetStage == 0 && snowStage == 0;
    }

    public boolean hasCoat() {
        return wetStage > 0 || snowStage > 0 || frostStage > 0;
    }

    public int lastSeenTick() {
        return lastSeenTick;
    }

    public void seen(int tick) {
        lastSeenTick = tick;
    }
}
