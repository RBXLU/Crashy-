package dev.pycodder.crashy.physics;

import dev.pycodder.crashy.Crashy;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Every call into Sable goes through here, so the rest of the mod stays readable and there is a
 * single place to look when Sable's API shifts.
 */
public final class SableBridge {

    private SableBridge() {
    }

    public static @Nullable ServerSubLevelContainer container(final ServerLevel level) {
        return SubLevelContainer.getContainer(level);
    }

    /** The sub-level whose plot contains {@code pos}, or {@code null} if that position is plain world. */
    public static @Nullable ServerSubLevel subLevelAt(final ServerLevel level, final BlockPos pos) {
        final SubLevelAccess access = Sable.HELPER.getContaining(level, pos);
        return access instanceof final ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved()
                ? serverSubLevel
                : null;
    }

    public static @Nullable ServerSubLevel byId(final ServerLevel level, final UUID uuid) {
        final ServerSubLevelContainer container = container(level);
        if (container == null) {
            return null;
        }
        for (final ServerSubLevel subLevel : container.getAllSubLevels()) {
            if (!subLevel.isRemoved() && uuid.equals(subLevel.getUniqueId())) {
                return subLevel;
            }
        }
        return null;
    }

    public static @Nullable RigidBodyHandle handle(final ServerSubLevel subLevel) {
        if (subLevel.isRemoved()) {
            return null;
        }
        try {
            final RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
            return handle != null && handle.isValid() ? handle : null;
        } catch (final RuntimeException e) {
            return null;
        }
    }

    /** World-space linear velocity in m/s, or zero if the body is gone. */
    public static Vector3d linearVelocity(final ServerSubLevel subLevel, final Vector3d dest) {
        final RigidBodyHandle handle = handle(subLevel);
        return handle == null ? dest.zero() : handle.getLinearVelocity(dest);
    }

    /** World-space angular velocity in rad/s, or zero if the body is gone. */
    public static Vector3d angularVelocity(final ServerSubLevel subLevel, final Vector3d dest) {
        final RigidBodyHandle handle = handle(subLevel);
        return handle == null ? dest.zero() : handle.getAngularVelocity(dest);
    }

    /**
     * Overwrites the world-space linear velocity and adds a spin. Sable only exposes additive
     * velocity changes, so the current velocity is subtracted out first.
     */
    public static void setVelocity(final ServerSubLevel subLevel, final Vector3dc linear, final Vector3dc angular) {
        final RigidBodyHandle handle = handle(subLevel);
        if (handle == null) {
            return;
        }
        final Vector3d currentLinear = handle.getLinearVelocity(new Vector3d());
        final Vector3d currentAngular = handle.getAngularVelocity(new Vector3d());
        handle.addLinearAndAngularVelocity(
                linear.sub(currentLinear, new Vector3d()),
                angular.sub(currentAngular, new Vector3d()));
    }

    public static void addVelocity(final ServerSubLevel subLevel, final Vector3dc linear, final Vector3dc angular) {
        final RigidBodyHandle handle = handle(subLevel);
        if (handle != null) {
            handle.addLinearAndAngularVelocity(linear, angular);
        }
    }

    /** Total mass of the body in kg, or 0 when it has no valid mass data. */
    public static double mass(final ServerSubLevel subLevel) {
        try {
            return subLevel.getMassTracker().getMass();
        } catch (final RuntimeException e) {
            return 0.0;
        }
    }

    /** Turns a set of world blocks into one rigid body. Returns {@code null} if Sable refused. */
    public static @Nullable ServerSubLevel assemble(final ServerLevel level,
                                                    final BlockPos anchor,
                                                    final Collection<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return null;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (final BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        // Sable wants the bounds inflated by one so entities and tracking points on the surface come along.
        final BoundingBox3i bounds = new BoundingBox3i(minX - 1, minY - 1, minZ - 1, maxX + 1, maxY + 1, maxZ + 1);

        try {
            final ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, bounds);
            if (subLevel == null || subLevel.isRemoved() || subLevel.getMassTracker().isInvalid()) {
                return null;
            }
            return subLevel;
        } catch (final RuntimeException e) {
            Crashy.LOGGER.error("Failed to assemble {} blocks at {}", blocks.size(), anchor, e);
            return null;
        }
    }

    /**
     * Breaks a single block out into its own one-block rigid body. This is how Sable's own
     * {@code /sable assemble shatter} works, and it is valid both for world blocks and for blocks
     * that currently sit inside another sub-level's plot — in the latter case Sable hands the new
     * body the parent's momentum for us.
     */
    public static @Nullable ServerSubLevel shatterSingleBlock(final ServerLevel level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        return assemble(level, pos, List.of(pos));
    }

    /** Bounds of the body's blocks inside its plot (plot coordinates, not world coordinates). */
    public static @Nullable BoundingBox3ic plotBounds(final ServerSubLevel subLevel) {
        try {
            return subLevel.getPlot().getBoundingBox();
        } catch (final RuntimeException e) {
            return null;
        }
    }

    public static Vector3d position(final ServerSubLevel subLevel, final Vector3d dest) {
        return dest.set(subLevel.logicalPose().position());
    }

    /**
     * Deletes a body and everything inside its plot, without dropping anything.
     *
     * <p>Deliberately not {@code plot.destroyAllBlocks()}: that calls
     * {@code level.destroyBlock(pos, true)}, which spits the block out as an item. Every caller here
     * has already decided what happens to the block — placing it back in the world, or dropping it
     * exactly once — so a second copy from Sable would duplicate it.
     */
    public static void destroy(final ServerLevel level, final ServerSubLevel subLevel) {
        if (subLevel.isRemoved()) {
            return;
        }
        try {
            clearPlot(level, subLevel);
            subLevel.markRemoved();
        } catch (final RuntimeException e) {
            Crashy.LOGGER.warn("Failed to destroy sub-level {}", subLevel.getUniqueId(), e);
        }
    }

    /** Silently empties a plot. Sable removes the now massless body on its next tick. */
    public static void clearPlot(final ServerLevel level, final ServerSubLevel subLevel) {
        final BoundingBox3ic bounds = plotBounds(subLevel);
        if (bounds == null) {
            return;
        }
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).isAir()) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** Every non-air block inside a body's plot, in plot coordinates. */
    public static List<BlockPos> blocksInPlot(final ServerLevel level, final ServerSubLevel subLevel) {
        final BoundingBox3ic bounds = plotBounds(subLevel);
        if (bounds == null) {
            return List.of();
        }
        final List<BlockPos> blocks = new ArrayList<>();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    final BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        blocks.add(pos);
                    }
                }
            }
        }
        return blocks;
    }

    /** Maps a position inside a body's plot to where it currently is in the world. */
    public static Vector3d plotToWorld(final ServerSubLevel subLevel, final BlockPos plotPos, final Vector3d dest) {
        dest.set(plotPos.getX() + 0.5, plotPos.getY() + 0.5, plotPos.getZ() + 0.5);
        return subLevel.logicalPose().transformPosition(dest);
    }
}
