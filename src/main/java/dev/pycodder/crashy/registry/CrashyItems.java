package dev.pycodder.crashy.registry;

import dev.pycodder.crashy.Crashy;
import dev.pycodder.crashy.block.PhysicsActivatorItem;
import dev.pycodder.crashy.glue.GlueItem;
import dev.pycodder.crashy.launcher.LauncherItem;
import dev.pycodder.crashy.structure.BlueprintItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrashyItems {

    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(Crashy.MOD_ID);

    /** Construction glue: bonds blocks into one rigid group. */
    public static final DeferredItem<GlueItem> GLUE = REGISTRY.register(
            "glue",
            () -> new GlueItem(new Item.Properties().stacksTo(1).durability(1024)));

    /** Gravity-gun style launcher. */
    public static final DeferredItem<LauncherItem> LAUNCHER = REGISTRY.register(
            "launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).durability(512)));

    public static final DeferredItem<PhysicsActivatorItem> PHYSICS_ACTIVATOR = REGISTRY.register(
            "physics_activator",
            () -> new PhysicsActivatorItem(CrashyBlocks.PHYSICS_ACTIVATOR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> BUILD_SAVER = REGISTRY.register(
            "build_saver",
            () -> new BlockItem(CrashyBlocks.BUILD_SAVER.get(), new Item.Properties()));

    /** Carries a saved build. Never stacks — each one holds its own structure. */
    public static final DeferredItem<BlueprintItem> BLUEPRINT = REGISTRY.register(
            "blueprint",
            () -> new BlueprintItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private CrashyItems() {
    }
}
