package dev.pycodder.crashy.client;

import dev.pycodder.crashy.Crashy;
import dev.pycodder.crashy.settings.CrashySettingsData;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Crashy.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CrashyClientEvents {

    private CrashyClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();

        while (CrashyKeybinds.OPEN_SETTINGS.consumeClick()) {
            if (minecraft.level != null && minecraft.screen == null) {
                minecraft.setScreen(new CrashySettingsScreen());
            }
        }
    }

    /** Leaving a world must not carry one server's rules into the next. */
    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        GlueHighlightState.clear();
        ClientSettings.accept(CrashySettingsData.DEFAULT, false);
    }
}
