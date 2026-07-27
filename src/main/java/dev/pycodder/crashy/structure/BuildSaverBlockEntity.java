package dev.pycodder.crashy.structure;

import dev.pycodder.crashy.registry.CrashyBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Holds the build a {@link BuildSaverBlock} has recorded. */
public class BuildSaverBlockEntity extends BlockEntity {

    private static final String KEY = "structure";

    private @Nullable StructureData structure;

    public BuildSaverBlockEntity(final BlockPos pos, final BlockState state) {
        super(CrashyBlockEntities.BUILD_SAVER.get(), pos, state);
    }

    public @Nullable StructureData getStructure() {
        return this.structure;
    }

    public void setStructure(final @Nullable StructureData structure) {
        this.structure = structure;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.structure != null && !this.structure.isEmpty()) {
            StructureData.CODEC.encodeStart(NbtOps.INSTANCE, this.structure)
                    .result()
                    .ifPresent(encoded -> tag.put(KEY, encoded));
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final Tag encoded = tag.get(KEY);
        this.structure = encoded == null
                ? null
                : StructureData.CODEC.parse(NbtOps.INSTANCE, encoded).result().orElse(null);
    }
}
