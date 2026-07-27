package dev.pycodder.crashy.registry;

import com.mojang.serialization.Codec;
import dev.pycodder.crashy.Crashy;
import dev.pycodder.crashy.structure.StructureData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrashyComponents {

    public static final DeferredRegister<DataComponentType<?>> REGISTRY =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Crashy.MOD_ID);

    /**
     * Set on the launcher while it holds an object. Data components are synced to the client for
     * free, so the client knows whether right-click should start the charge-up animation.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GRABBING =
            REGISTRY.register("grabbing", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** The build a blueprint is carrying. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StructureData>> STRUCTURE =
            REGISTRY.register("structure", () -> DataComponentType.<StructureData>builder()
                    .persistent(StructureData.CODEC)
                    .networkSynchronized(StructureData.STREAM_CODEC)
                    .build());

    private CrashyComponents() {
    }
}
