package org.lts.weathercoat.track;

import org.lts.weathercoat.WeatherCoatConfig;

public final class Exclusions {
    private Exclusions() {
    }

    public static boolean isAffected(boolean aquatic, boolean fireproof, String id, WeatherCoatConfig config) {
        if (config.skipAquatic && aquatic) {
            return false;
        }
        if (config.skipFireproof && fireproof) {
            return false;
        }
        return !config.excludedEntities.contains(id);
    }

    public static String signature(WeatherCoatConfig config) {
        return config.affectPlayers + "|" + config.skipAquatic + "|" + config.skipFireproof + "|" + config.excludedEntities;
    }
}
