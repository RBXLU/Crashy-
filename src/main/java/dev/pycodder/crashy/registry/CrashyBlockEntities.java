package dev.pycodder.crashy.registry;

import dev.pycodder.crashy.Crashy;
import dev.pycodder.crashy.structure.BuildSaverBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrashyBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTRY =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Crashy.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BuildSaverBlockEntity>> BUILD_SAVER =
            REGISTRY.register("build_saver", () -> BlockEntityType.Builder
                    .of(BuildSaverBlockEntity::new, CrashyBlocks.BUILD_SAVER.get())
                    .build(null));

    private CrashyBlockEntities() {
    }
}
