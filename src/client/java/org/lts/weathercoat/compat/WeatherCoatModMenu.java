package org.lts.weathercoat.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.lts.weathercoat.gui.WeatherCoatConfigScreen;

public final class WeatherCoatModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WeatherCoatConfigScreen::new;
    }
}
