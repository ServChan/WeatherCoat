package org.lts.weathercoat.track;

import org.lts.weathercoat.paint.GroundPainter;

public final class GroundWeather {
    static final float PUDDLE_FILL_SECONDS = 120.0F;
    static final float PUDDLE_DRY_SECONDS = 300.0F;
    static final float SNOW_FILL_SECONDS = 150.0F;
    static final float SNOW_MELT_SECONDS = 420.0F;

    private float puddles;
    private float snow;

    public void advance(float seconds, float intensity, float dryFactor, float meltFactor, boolean puddlesOn, boolean snowOn) {
        puddles = puddlesOn ? step(puddles, seconds, intensity, PUDDLE_FILL_SECONDS, dryFactor, PUDDLE_DRY_SECONDS) : 0.0F;
        snow = snowOn ? step(snow, seconds, intensity, SNOW_FILL_SECONDS, meltFactor, SNOW_MELT_SECONDS) : 0.0F;
    }

    private static float step(float level, float seconds, float intensity, float fill, float fadeFactor, float fade) {
        float delta = intensity > 0.0F ? seconds * intensity / fill : -seconds * fadeFactor / fade;
        return Math.max(0.0F, Math.min(1.0F, level + delta));
    }

    public void fill(boolean puddlesOn, boolean snowOn) {
        puddles = puddlesOn ? 1.0F : 0.0F;
        snow = snowOn ? 1.0F : 0.0F;
    }

    public void reset() {
        puddles = 0.0F;
        snow = 0.0F;
    }

    public float puddles() {
        return puddles;
    }

    public float snow() {
        return snow;
    }

    public int puddleStage() {
        return stage(puddles);
    }

    public int snowStage() {
        return stage(snow);
    }

    static int stage(float level) {
        return GroundPainter.clampStage((int) Math.floor(level * GroundPainter.STAGES + 1.0E-4F));
    }
}
