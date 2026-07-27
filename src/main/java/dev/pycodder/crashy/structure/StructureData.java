package dev.pycodder.crashy.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pycodder.crashy.glue.GlueSavedData;
import dev.pycodder.crashy.physics.SableBridge;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A saved build: block states plus their offsets from the device that recorded them.
 *
 * <p>Offsets are relative to the recording block, so pasting from a blueprint puts the structure in
 * the same place relative to where you click that it was relative to the saver.
 */
public record StructureData(List<Entry> blocks) {

    public record Entry(BlockPos offset, BlockState state) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::offset),
                BlockState.CODEC.fieldOf("state").forGetter(Entry::state)
        ).apply(instance, Entry::new));
    }

    public static final Codec<StructureData> CODEC =
            Entry.CODEC.listOf().xmap(StructureData::new, StructureData::blocks);

    public static final StreamCodec<ByteBuf, StructureData> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    public int size() {
        return this.blocks.size();
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

    /** Width x height x depth of the recorded build, for tooltips. */
    public Vec3i dimensions() {
        if (this.blocks.isEmpty()) {
            return Vec3i.ZERO;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (final Entry entry : this.blocks) {
            final BlockPos pos = entry.offset();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
    }

    /** Records the given world positions relative to {@code origin}. */
    public static StructureData capture(final ServerLevel level,
                                        final Collection<BlockPos> positions,
                                        final BlockPos origin) {
        final List<Entry> entries = new ArrayList<>(positions.size());
        for (final BlockPos pos : positions) {
            final BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            entries.add(new Entry(pos.subtract(origin), state));
        }
        return new StructureData(entries);
    }

    /**
     * Stamps the build back into the world.
     *
     * <p>Occupied spots are skipped rather than overwritten — pasting should never quietly eat
     * somebody's existing build. Everything placed comes out glued, so it can go straight into the
     * physics activator.
     *
     * @return how many blocks were actually placed
     */
    public int paste(final ServerLevel level, final BlockPos origin, final boolean glueResult) {
        final GlueSavedData glue = glueResult ? GlueSavedData.get(level) : null;
        int placed = 0;

        for (final Entry entry : this.blocks) {
            final BlockPos target = origin.offset(entry.offset());

            if (level.isOutsideBuildHeight(target) || !level.isLoaded(target)) {
                continue;
            }
            if (SableBridge.subLevelAt(level, target) != null) {
                continue;
            }
            final BlockState existing = level.getBlockState(target);
            if (!existing.isAir() && !existing.canBeReplaced()) {
                continue;
            }

            level.setBlock(target, entry.state(), Block.UPDATE_ALL);
            if (glue != null) {
                glue.glue(target);
            }
            placed++;
        }
        return placed;
    }
}
