package dev.pycodder.crashy.registry;

import dev.pycodder.crashy.Crashy;
import dev.pycodder.crashy.block.PhysicsActivatorBlock;
import dev.pycodder.crashy.structure.BuildSaverBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrashyBlocks {

    public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(Crashy.MOD_ID);

    public static final DeferredBlock<PhysicsActivatorBlock> PHYSICS_ACTIVATOR = REGISTRY.register(
            "physics_activator",
            () -> new PhysicsActivatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.5F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)
                    // The model is a caged core, not a solid cube — without this the world would
                    // cull the faces behind it and light it as if it were opaque.
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(PhysicsActivatorBlock.TRIGGERED) ? 3 : 8)));

    public static final DeferredBlock<BuildSaverBlock> BUILD_SAVER = REGISTRY.register(
            "build_saver",
            () -> new BuildSaverBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(2.5F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER)
                    // A table on legs, so the same applies here.
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(BuildSaverBlock.LOADED) ? 9 : 2)));

    private CrashyBlocks() {
    }
}
