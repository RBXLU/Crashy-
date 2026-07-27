package dev.pycodder.crashy.structure;

import dev.pycodder.crashy.glue.GlueSavedData;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Works out which blocks belong to "the build" a given block is attached to.
 *
 * <p>Shared by the physics activator and the build saver so both answer that question the same way:
 * glue decides, and only if there is no glue at all does it fall back to plain connectivity.
 */
public final class StructureScanner {

    private StructureScanner() {
    }

    public record Result(Set<BlockPos> blocks, boolean tooMany) {
        public boolean isEmpty() {
            return this.blocks.isEmpty();
        }
    }

    /**
     * @param origin              the block the scan starts from (an activator, a saver, ...)
     * @param maxBlocks           hard cap; going over reports {@code tooMany} rather than truncating
     * @param allowConnectedFallback whether an unglued build may be gathered by connectivity instead
     */
    public static Result collect(final ServerLevel level,
                                 final BlockPos origin,
                                 final int maxBlocks,
                                 final boolean allowConnectedFallback) {
        final GlueSavedData glue = GlueSavedData.get(level);
        final Set<BlockPos> blocks = new LinkedHashSet<>();

        final Set<BlockPos> own = glue.getGroup(origin);
        if (own != null) {
            blocks.addAll(own);
        }

        // The origin block itself does not have to be glued: any glued neighbour pulls in its whole
        // group, which is what players expect when they slap a device onto a finished build.
        for (final Direction direction : Direction.values()) {
            final BlockPos neighbour = origin.relative(direction);
            if (level.getBlockState(neighbour).isAir()) {
                continue;
            }
            final Set<BlockPos> group = glue.getGroup(neighbour);
            if (group != null) {
                blocks.addAll(group);
            }
        }

        if (blocks.size() < 2 && allowConnectedFallback) {
            final SubLevelAssemblyHelper.GatherResult gathered =
                    SubLevelAssemblyHelper.gatherConnectedBlocks(origin, level, maxBlocks, null);

            if (gathered.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS
                    || gathered.blocks() == null) {
                return new Result(Set.of(), true);
            }
            blocks.clear();
            blocks.addAll(gathered.blocks());
        }

        blocks.removeIf(pos -> level.getBlockState(pos).isAir());

        if (blocks.size() > maxBlocks) {
            return new Result(Set.of(), true);
        }
        return new Result(blocks, false);
    }
}
