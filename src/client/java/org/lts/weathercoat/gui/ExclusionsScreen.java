package org.lts.weathercoat.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import org.lts.weathercoat.WeatherCoatConfig;

public final class ExclusionsScreen extends Screen {
    private static final int HEADER = 33;
    private static final int FOOTER = 33;
    private static final int ROW_WIDTH = 310;

    private final Screen parent;
    private final WeatherCoatConfig working;

    public ExclusionsScreen(Screen parent, WeatherCoatConfig working) {
        super(Component.translatable("weathercoat.screen.excluded.title"));
        this.parent = parent;
        this.working = working;
    }

    @Override
    protected void init() {
        OptionList list = new OptionList(this.minecraft, ROW_WIDTH);
        list.place(0, HEADER, this.width, this.height - HEADER - FOOTER);
        list.text(Component.translatable("weathercoat.excluded.hint"));
        for (EntityType<?> type : livingTypes()) {
            String id = EntityType.getKey(type).toString();
            Component skipped = automaticReason(type);
            Component hint = skipped != null ? skipped : Component.literal(id);
            list.toggle(type.getDescription(), hint,
                    () -> skipped == null && !working.excludedEntities.contains(id),
                    value -> setAffected(id, value),
                    () -> skipped == null);
        }
        list.refresh();
        addRenderableWidget(list);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    private void setAffected(String id, boolean affected) {
        List<String> ids = new ArrayList<>(working.excludedEntities);
        ids.remove(id);
        if (!affected) {
            ids.add(id);
        }
        working.excludedEntities = ids;
    }

    private Component automaticReason(EntityType<?> type) {
        MobCategory category = type.getCategory();
        boolean aquatic = category == MobCategory.WATER_CREATURE || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE || category == MobCategory.AXOLOTLS;
        if (working.skipAquatic && aquatic) {
            return Component.translatable("weathercoat.excluded.aquatic");
        }
        if (working.skipFireproof && type.fireImmune()) {
            return Component.translatable("weathercoat.excluded.fireproof");
        }
        return null;
    }

    private static List<EntityType<?>> livingTypes() {
        List<EntityType<?>> types = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityTypes.PLAYER || type == EntityTypes.ARMOR_STAND) {
                continue;
            }
            if (LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
                types.add(type);
            }
        }
        types.sort(Comparator.comparing(type -> type.getDescription().getString()));
        return types;
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
