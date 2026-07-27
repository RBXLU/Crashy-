package dev.pycodder.crashy.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.pycodder.crashy.Crashy;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Crashy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CrashyKeybinds {

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.crashy.settings",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            "key.categories.crashy");

    private CrashyKeybinds() {
    }

    @SubscribeEvent
    public static void register(final RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
    }
}
