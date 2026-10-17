package org.lts.weathercoat.track;

public final class Climate {
    public static final float HOT_TEMPERATURE = 1.5F;
    static final float HOT_FACTOR = 2.0F;
    static final float HUMID_FACTOR = 0.6F;
    static final float SUN_FACTOR = 1.5F;
    static final float HEAT_FACTOR = 4.0F;
    static final float WARM_MELT_FACTOR = 2.0F;
    static final float SHAKE_MELT_FACTOR = 1.5F;

    private Climate() {
    }

    public static float dryFactor(float temperature, boolean humid, boolean sunny, boolean nearHeat) {
        float factor = 1.0F;
        if (temperature >= HOT_TEMPERATURE) {
            factor *= HOT_FACTOR;
        }
        if (humid) {
            factor *= HUMID_FACTOR;
        }
        if (sunny) {
            factor *= SUN_FACTOR;
        }
        if (nearHeat) {
            factor *= HEAT_FACTOR;
        }
        return factor;
    }

    public static float meltFactor(boolean warm, float temperature, boolean sunny, boolean nearHeat, boolean moving) {
        float factor = warm ? WARM_MELT_FACTOR : 1.0F;
        if (temperature >= HOT_TEMPERATURE) {
            factor *= HOT_FACTOR;
        }
        if (sunny) {
            factor *= SUN_FACTOR;
        }
        if (nearHeat) {
            factor *= HEAT_FACTOR;
        }
        if (moving) {
            factor *= SHAKE_MELT_FACTOR;
        }
        return factor;
    }

    public static int frostStage(float percentFrozen, int stages) {
        if (!(percentFrozen > 0.0F)) {
            return 0;
        }
        return Math.max(1, Math.min(stages, Math.round(Math.min(1.0F, percentFrozen) * stages)));
    }
}
