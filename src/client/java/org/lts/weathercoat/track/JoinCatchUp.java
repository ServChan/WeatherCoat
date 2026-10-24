package org.lts.weathercoat.track;

public final class JoinCatchUp {
    static final int WINDOW_TICKS = 40;
    static final float RAIN_LEVEL = 0.5F;

    private int ticks;
    private boolean done;

    public void reset() {
        ticks = 0;
        done = false;
    }

    public boolean tick(float rainLevel) {
        ticks++;
        if (done || ticks > WINDOW_TICKS) {
            return false;
        }
        if (rainLevel > RAIN_LEVEL) {
            done = true;
            return true;
        }
        return false;
    }
}
