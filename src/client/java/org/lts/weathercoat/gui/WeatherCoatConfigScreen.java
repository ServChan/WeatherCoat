package org.lts.weathercoat.gui;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lts.weathercoat.WeatherCoatConfig;

public final class WeatherCoatConfigScreen extends Screen {
    private static final int HEADER = 33;
    private static final int FOOTER = 33;
    private static final int ROW_WIDTH = 310;
    private static final List<Integer> STAGES = List.of(3, 4, 5);
    private static final List<Integer> GROW_SECONDS = List.of(5, 10, 15, 20, 30, 45, 60, 90, 120);
    private static final List<Integer> FADE_SECONDS = List.of(10, 20, 30, 40, 60, 90, 120, 180, 300);
    private static final List<Integer> DISTANCES = List.of(16, 32, 48, 64, 96, 128, 192, 256);
    private static final List<Integer> GROUND_RADII = List.of(16, 24, 32, 48, 64);

    private final Screen parent;
    private WeatherCoatConfig working;
    private OptionList list;

    public WeatherCoatConfigScreen(Screen parent) {
        super(Component.translatable("weathercoat.screen.title"));
        this.parent = parent;
        this.working = WeatherCoatConfig.currentCopy();
    }

    @Override
    protected void init() {
        list = new OptionList(this.minecraft, ROW_WIDTH);
        list.place(0, HEADER, this.width, this.height - HEADER - FOOTER);

        list.header(Component.translatable("weathercoat.section.general"));
        list.toggle(Component.translatable("weathercoat.option.enabled"), Component.translatable("weathercoat.option.enabled.tip"),
                () -> working.enabled, value -> working.enabled = value, null);
        list.toggle(Component.translatable("weathercoat.option.players"), Component.translatable("weathercoat.option.players.tip"),
                () -> working.affectPlayers, value -> working.affectPlayers = value, () -> working.enabled);
        list.cycle(Component.translatable("weathercoat.option.stages"), Component.translatable("weathercoat.option.stages.tip"),
                STAGES, value -> Component.literal(Integer.toString(value)),
                () -> working.stages, value -> working.stages = value, () -> working.enabled);
        list.toggle(Component.translatable("weathercoat.option.catch_up"), Component.translatable("weathercoat.option.catch_up.tip"),
                () -> working.catchUpOnSight, value -> working.catchUpOnSight = value, () -> working.enabled);
        list.cycle(Component.translatable("weathercoat.option.distance"), Component.translatable("weathercoat.option.distance.tip"),
                withCurrent(DISTANCES, working.maxDistance), value -> Component.translatable("weathercoat.unit.blocks", value),
                () -> working.maxDistance, value -> working.maxDistance = value, () -> working.enabled);

        list.header(Component.translatable("weathercoat.section.rain"));
        list.toggle(Component.translatable("weathercoat.option.rain"), Component.translatable("weathercoat.option.rain.tip"),
                () -> working.rainEnabled, value -> working.rainEnabled = value, () -> working.enabled);
        seconds("weathercoat.option.wet_time", GROW_SECONDS, () -> working.wetSecondsPerStage,
                value -> working.wetSecondsPerStage = value, () -> working.enabled && working.rainEnabled);
        seconds("weathercoat.option.dry_time", FADE_SECONDS, () -> working.drySecondsPerStage,
                value -> working.drySecondsPerStage = value, () -> working.enabled && working.rainEnabled);

        list.header(Component.translatable("weathercoat.section.snow"));
        list.toggle(Component.translatable("weathercoat.option.snow"), Component.translatable("weathercoat.option.snow.tip"),
                () -> working.snowEnabled, value -> working.snowEnabled = value, () -> working.enabled);
        seconds("weathercoat.option.snow_time", GROW_SECONDS, () -> working.snowSecondsPerStage,
                value -> working.snowSecondsPerStage = value, () -> working.enabled && working.snowEnabled);
        seconds("weathercoat.option.melt_time", FADE_SECONDS, () -> working.meltSecondsPerStage,
                value -> working.meltSecondsPerStage = value, () -> working.enabled && working.snowEnabled);
        list.toggle(Component.translatable("weathercoat.option.frost"), Component.translatable("weathercoat.option.frost.tip"),
                () -> working.frostEnabled, value -> working.frostEnabled = value, () -> working.enabled);

        list.header(Component.translatable("weathercoat.section.ground"));
        list.toggle(Component.translatable("weathercoat.option.puddles"), Component.translatable("weathercoat.option.puddles.tip"),
                () -> working.puddlesEnabled, value -> working.puddlesEnabled = value, () -> working.enabled);
        list.toggle(Component.translatable("weathercoat.option.ground_snow"), Component.translatable("weathercoat.option.ground_snow.tip"),
                () -> working.groundSnowEnabled, value -> working.groundSnowEnabled = value, () -> working.enabled);
        list.cycle(Component.translatable("weathercoat.option.ground_radius"), Component.translatable("weathercoat.option.ground_radius.tip"),
                withCurrent(GROUND_RADII, working.groundRadius), value -> Component.translatable("weathercoat.unit.blocks", value),
                () -> working.groundRadius, value -> working.groundRadius = value,
                () -> working.enabled && (working.puddlesEnabled || working.groundSnowEnabled));

        list.header(Component.translatable("weathercoat.section.world"));
        list.toggle(Component.translatable("weathercoat.option.climate"), Component.translatable("weathercoat.option.climate.tip"),
                () -> working.climateEnabled, value -> working.climateEnabled = value, () -> working.enabled);
        list.toggle(Component.translatable("weathercoat.option.particles"), Component.translatable("weathercoat.option.particles.tip"),
                () -> working.particlesEnabled, value -> working.particlesEnabled = value, () -> working.enabled);

        list.header(Component.translatable("weathercoat.section.mobs"));
        list.toggle(Component.translatable("weathercoat.option.skip_aquatic"), Component.translatable("weathercoat.option.skip_aquatic.tip"),
                () -> working.skipAquatic, value -> working.skipAquatic = value, () -> working.enabled);
        list.toggle(Component.translatable("weathercoat.option.skip_fireproof"), Component.translatable("weathercoat.option.skip_fireproof.tip"),
                () -> working.skipFireproof, value -> working.skipFireproof = value, () -> working.enabled);
        list.action(Component.translatable("weathercoat.option.excluded"), Component.translatable("weathercoat.option.excluded.tip"),
                () -> Component.translatable("weathercoat.excluded.count", working.excludedEntities.size()),
                () -> this.minecraft.setScreenAndShow(new ExclusionsScreen(this, working)), () -> working.enabled);

        list.refresh();
        addRenderableWidget(list);

        int gap = 8;
        int buttonWidth = Math.min(100, (this.width - 24 - gap * 2) / 3);
        int left = (this.width - buttonWidth * 3 - gap * 2) / 2;
        int y = this.height - 27;
        addRenderableWidget(Button.builder(Component.translatable("weathercoat.button.reset"), button -> resetDefaults())
                .bounds(left, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("weathercoat.button.save"), button -> saveAndClose())
                .bounds(left + buttonWidth + gap, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(left + (buttonWidth + gap) * 2, y, buttonWidth, 20).build());
    }

    private void seconds(String key, List<Integer> values, IntSupplier value, Consumer<Integer> change,
            BooleanSupplier active) {
        list.cycle(Component.translatable(key), Component.translatable(key + ".tip"),
                withCurrent(values, value.getAsInt()), seconds -> Component.translatable("weathercoat.unit.seconds", seconds),
                value::getAsInt, change, active);
    }

    private static List<Integer> withCurrent(List<Integer> values, int current) {
        if (values.contains(current)) {
            return values;
        }
        return Stream.concat(values.stream(), Stream.of(current)).sorted().toList();
    }

    private void resetDefaults() {
        working = WeatherCoatConfig.defaults();
        rebuildWidgets();
    }

    private void saveAndClose() {
        WeatherCoatConfig.save(working);
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        boolean inWorld = this.minecraft.level != null;
        graphics.blit(RenderPipelines.GUI_TEXTURED, inWorld ? Screen.INWORLD_HEADER_SEPARATOR : Screen.HEADER_SEPARATOR,
                0, HEADER - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, inWorld ? Screen.INWORLD_FOOTER_SEPARATOR : Screen.FOOTER_SEPARATOR,
                0, this.height - FOOTER, 0.0F, 0.0F, this.width, 2, 32, 2);
        graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }
}
