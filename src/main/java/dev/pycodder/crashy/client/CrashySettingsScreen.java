package dev.pycodder.crashy.client;

import dev.pycodder.crashy.network.SettingsUpdatePayload;
import dev.pycodder.crashy.settings.CrashySettingsData;
import dev.pycodder.crashy.settings.DestructionMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.DoubleConsumer;

/** The F7 panel: how destructive this world is allowed to be. */
public class CrashySettingsScreen extends Screen {

    private static final int WIDGET_WIDTH = 190;
    private static final int WIDGET_HEIGHT = 20;
    private static final int GAP = 4;

    private CrashySettingsData working;
    private final boolean editable;

    public CrashySettingsScreen() {
        super(Component.translatable("screen.crashy.settings"));
        this.working = ClientSettings.get();
        this.editable = ClientSettings.canEdit();
    }

    @Override
    protected void init() {
        final int left = this.width / 2 - WIDGET_WIDTH - GAP / 2;
        final int right = this.width / 2 + GAP / 2;
        int y = 52;

        this.addRenderableWidget(CycleButton.<DestructionMode>builder(DestructionMode::displayName)
                .withValues(DestructionMode.values())
                .withInitialValue(this.working.mode())
                .withTooltip(mode -> Tooltip.create(mode.description()))
                .create(left, y, WIDGET_WIDTH * 2 + GAP, WIDGET_HEIGHT,
                        Component.translatable("settings.crashy.mode"),
                        (button, value) -> this.working = this.working.withMode(value)))
                .active = this.editable;

        y += WIDGET_HEIGHT + GAP * 2;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.working.destroyWorld())
                .create(left, y, WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("settings.crashy.destroy_world"),
                        (button, value) -> this.working = this.working.withDestroyWorld(value)))
                .active = this.editable;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.working.tntBlasts())
                .create(right, y, WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("settings.crashy.tnt"),
                        (button, value) -> this.working = this.working.withTntBlasts(value)))
                .active = this.editable;

        y += WIDGET_HEIGHT + GAP;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.working.settleDebris())
                .create(left, y, WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.translatable("settings.crashy.settle_debris"),
                        (button, value) -> this.working = this.working.withSettleDebris(value)))
                .active = this.editable;

        this.addRenderableWidget(slider(right, y, "settings.crashy.toughness",
                CrashySettingsData.MIN_TOUGHNESS, CrashySettingsData.MAX_TOUGHNESS,
                this.working.toughnessScale(),
                value -> this.working = this.working.withToughnessScale(value)))
                .active = this.editable;

        y += WIDGET_HEIGHT + GAP;

        this.addRenderableWidget(slider(left, y, "settings.crashy.radius",
                CrashySettingsData.MIN_SCALE, CrashySettingsData.MAX_RADIUS_SCALE,
                this.working.radiusScale(),
                value -> this.working = this.working.withRadiusScale(value)))
                .active = this.editable;

        this.addRenderableWidget(slider(right, y, "settings.crashy.scatter",
                CrashySettingsData.MIN_SCALE, CrashySettingsData.MAX_SCATTER_SCALE,
                this.working.scatterScale(),
                value -> this.working = this.working.withScatterScale(value)))
                .active = this.editable;

        y += WIDGET_HEIGHT + GAP;

        this.addRenderableWidget(slider(left, y, "settings.crashy.speed",
                CrashySettingsData.MIN_SPEED_PER_POWER, CrashySettingsData.MAX_SPEED_PER_POWER,
                this.working.speedPerPower(),
                value -> this.working = this.working.withSpeedPerPower(value)))
                .active = this.editable;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("settings.crashy.reset"),
                        button -> {
                            this.working = CrashySettingsData.DEFAULT;
                            this.rebuildWidgets();
                        })
                .bounds(right, y, WIDGET_WIDTH, WIDGET_HEIGHT)
                .build())
                .active = this.editable;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 32, 200, WIDGET_HEIGHT)
                .build());
    }

    private AbstractSliderButton slider(final int x, final int y, final String key,
                                        final double min, final double max, final double value,
                                        final DoubleConsumer setter) {
        return new DoubleSlider(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, key, min, max, value, setter);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        graphics.drawCenteredString(this.font, this.working.mode().description(),
                this.width / 2, 30, 0xA0A0A0);

        if (!this.editable) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("settings.crashy.no_permission").withStyle(ChatFormatting.RED),
                    this.width / 2, this.height - 46, 0xFF6666);
        }
    }

    /** Settings are applied on close, in one packet, rather than on every slider tick. */
    @Override
    public void onClose() {
        if (this.editable && !this.working.equals(ClientSettings.get())) {
            PacketDistributor.sendToServer(new SettingsUpdatePayload(this.working));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class DoubleSlider extends AbstractSliderButton {

        private final String key;
        private final double min;
        private final double max;
        private final DoubleConsumer setter;

        private DoubleSlider(final int x, final int y, final int width, final int height,
                             final String key, final double min, final double max,
                             final double initial, final DoubleConsumer setter) {
            super(x, y, width, height, Component.empty(),
                    Mth.clamp((initial - min) / (max - min), 0.0, 1.0));
            this.key = key;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.updateMessage();
        }

        private double current() {
            return this.min + this.value * (this.max - this.min);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable(this.key, String.format("%.2f", this.current())));
        }

        @Override
        protected void applyValue() {
            this.setter.accept(this.current());
        }
    }
}
