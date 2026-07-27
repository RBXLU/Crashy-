package dev.pycodder.crashy;

import dev.pycodder.crashy.glue.GlueEvents;
import dev.pycodder.crashy.launcher.GrabManager;
import dev.pycodder.crashy.physics.DebrisManager;
import dev.pycodder.crashy.physics.ImpactTracker;
import dev.pycodder.crashy.physics.ShatterQueue;
import dev.pycodder.crashy.registry.CrashyBlockEntities;
import dev.pycodder.crashy.registry.CrashyBlocks;
import dev.pycodder.crashy.registry.CrashyComponents;
import dev.pycodder.crashy.registry.CrashyCreativeTab;
import dev.pycodder.crashy.registry.CrashyItems;
import dev.pycodder.crashy.registry.CrashySounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crashy! — realistic destruction built on top of the Sable sub-level physics library.
 *
 * <p>The gameplay loop is:
 * <ol>
 *     <li>Hold {@link CrashyItems#GLUE} in the off-hand and build — every placed block is glued to
 *         its already-glued neighbours, forming one rigid group.</li>
 *     <li>Place a {@link CrashyBlocks#PHYSICS_ACTIVATOR} on the build and activate it. The glued
 *         group becomes a Sable sub-level, i.e. a real rigid body.</li>
 *     <li>Grab that body with the {@link CrashyItems#LAUNCHER}, hold right-click to charge up to
 *         power 10, and let go. Whatever it hits — and the object itself — shatters into loose
 *         physics blocks that eventually settle back into the world.</li>
 * </ol>
 */
@Mod(Crashy.MOD_ID)
public class Crashy {

    public static final String MOD_ID = "crashy";
    public static final Logger LOGGER = LoggerFactory.getLogger("Crashy!");

    public Crashy(final IEventBus modBus, final ModContainer container) {
        CrashyComponents.REGISTRY.register(modBus);
        CrashyBlocks.REGISTRY.register(modBus);
        CrashyItems.REGISTRY.register(modBus);
        CrashyBlockEntities.REGISTRY.register(modBus);
        CrashySounds.REGISTRY.register(modBus);
        CrashyCreativeTab.REGISTRY.register(modBus);

        modBus.addListener(CrashyCreativeTab::onBuildCreativeTabs);

        NeoForge.EVENT_BUS.register(GlueEvents.class);
        NeoForge.EVENT_BUS.register(GrabManager.class);
        NeoForge.EVENT_BUS.register(ImpactTracker.class);
        NeoForge.EVENT_BUS.register(ShatterQueue.class);
        NeoForge.EVENT_BUS.register(DebrisManager.class);

        container.registerConfig(ModConfig.Type.SERVER, CrashyConfig.SPEC);
    }

    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
