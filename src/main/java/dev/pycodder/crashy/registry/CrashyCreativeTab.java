package dev.pycodder.crashy.registry;

import dev.pycodder.crashy.Crashy;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrashyCreativeTab {

    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Crashy.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = REGISTRY.register(
            "crashy",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.crashy"))
                    .icon(() -> new ItemStack(CrashyItems.LAUNCHER.get()))
                    .displayItems((params, output) -> {
                        output.accept(CrashyItems.GLUE.get());
                        output.accept(CrashyItems.PHYSICS_ACTIVATOR.get());
                        output.accept(CrashyItems.LAUNCHER.get());
                        output.accept(CrashyItems.BUILD_SAVER.get());
                        output.accept(CrashyItems.BLUEPRINT.get());
                    })
                    .build());

    /** Also surface the items in the vanilla tabs so they are easy to find. */
    public static void onBuildCreativeTabs(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CrashyItems.GLUE.get());
            event.accept(CrashyItems.LAUNCHER.get());
            event.accept(CrashyItems.BLUEPRINT.get());
        } else if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(CrashyItems.PHYSICS_ACTIVATOR.get());
            event.accept(CrashyItems.BUILD_SAVER.get());
        }
    }

    private CrashyCreativeTab() {
    }
}
