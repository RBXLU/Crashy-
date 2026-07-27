package dev.pycodder.crashy.physics;

import dev.pycodder.crashy.CrashyConfig;
import dev.pycodder.crashy.block.PhysicsActivatorBlock;
import dev.pycodder.crashy.glue.GlueSavedData;
import dev.pycodder.crashy.registry.CrashySounds;
import dev.pycodder.crashy.structure.StructureScanner;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Turns a glued build into a Sable rigid body. */
public final class PhysicsActivation {

    private PhysicsActivation() {
    }

    public static boolean activate(final ServerLevel level, final BlockPos activatorPos, final @Nullable Player player) {
        final BlockState activatorState = level.getBlockState(activatorPos);
        if (!(activatorState.getBlock() instanceof PhysicsActivatorBlock) || activatorState.getValue(PhysicsActivatorBlock.TRIGGERED)) {
            return false;
        }

        final GlueSavedData glue = GlueSavedData.get(level);
        final int maxBlocks = CrashyConfig.MAX_ASSEMBLE_BLOCKS.get();

        final StructureScanner.Result scan = StructureScanner.collect(
                level, activatorPos, maxBlocks, CrashyConfig.ACTIVATOR_FALLBACK_CONNECTED.get());

        if (scan.tooMany()) {
            fail(player, "message.crashy.activator.too_many", maxBlocks);
            return false;
        }

        final Set<BlockPos> blocks = scan.blocks();
        blocks.add(activatorPos);

        if (blocks.size() < 2) {
            fail(player, "message.crashy.activator.nothing", 0);
            return false;
        }

        // Flip the activator before assembling so the block carried into the plot shows as spent.
        level.setBlock(activatorPos, activatorState.setValue(PhysicsActivatorBlock.TRIGGERED, true), 3);

        final ServerSubLevel subLevel = SableBridge.assemble(level, activatorPos, blocks);
        if (subLevel == null) {
            level.setBlock(activatorPos, activatorState.setValue(PhysicsActivatorBlock.TRIGGERED, false), 3);
            fail(player, "message.crashy.activator.failed", 0);
            return false;
        }

        glue.forget(blocks);
        subLevel.setName("Crashy object");

        level.playSound(null, activatorPos, CrashySounds.ACTIVATE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        CrashyEffects.activate(level, activatorPos);

        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.crashy.activator.success", blocks.size()).withStyle(ChatFormatting.GREEN),
                    true);
        }
        return true;
    }

    /**
     * The inverse of {@link #activate}: freezes a physics object back into ordinary world blocks
     * wherever it currently is.
     *
     * <p>Positions are snapped to the block grid; a tilted object therefore lands straight. Block
     * states are kept as they are — rotating a staircase's facing to match an arbitrary quaternion
     * would be guesswork, and a build that comes back subtly rearranged is worse than one that
     * comes back upright. The result is glued back together so it can be activated again.
     */
    public static boolean deactivate(final ServerLevel level,
                                     final ServerSubLevel subLevel,
                                     final @Nullable Player player) {
        final List<BlockPos> plotBlocks = SableBridge.blocksInPlot(level, subLevel);
        if (plotBlocks.isEmpty()) {
            fail(player, "message.crashy.deactivate.empty", 0);
            return false;
        }

        // Work out where every block wants to land before touching the world, so overlapping
        // targets and blocked spots can be resolved without half-placing the build.
        final Map<BlockPos, BlockState> placements = new LinkedHashMap<>();
        final List<BlockState> homeless = new ArrayList<>();
        final Vector3d center = new Vector3d();

        for (final BlockPos plotPos : plotBlocks) {
            final BlockState state = level.getBlockState(plotPos);
            final Vector3d world = SableBridge.plotToWorld(subLevel, plotPos, new Vector3d());
            center.add(world);

            final BlockPos target = BlockPos.containing(world.x, world.y, world.z);

            if (placements.containsKey(target) || !canPlaceAt(level, target)) {
                homeless.add(state);
                continue;
            }
            placements.put(target, state);
        }
        center.div(plotBlocks.size());

        // Empty the plot first: its blocks and the world targets can overlap, and the plot copy
        // must not linger. Nothing is dropped here — this method decides what happens to each block.
        SableBridge.clearPlot(level, subLevel);
        subLevel.markRemoved();

        final GlueSavedData glue = GlueSavedData.get(level);
        for (final Map.Entry<BlockPos, BlockState> entry : placements.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL);
            glue.glue(entry.getKey());
        }

        // Whatever had nowhere to go goes to the player rather than vanishing.
        final BlockPos dropPos = BlockPos.containing(center.x, center.y, center.z);
        for (final BlockState state : homeless) {
            Block.popResource(level, dropPos, new ItemStack(state.getBlock()));
        }

        CrashyEffects.deactivate(level, center);
        level.playSound(null, dropPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8F, 1.4F);

        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.crashy.deactivate.success", placements.size())
                            .withStyle(ChatFormatting.GREEN),
                    true);
        }
        return true;
    }

    private static boolean canPlaceAt(final ServerLevel level, final BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
            return false;
        }
        if (SableBridge.subLevelAt(level, pos) != null) {
            return false;
        }
        final BlockState existing = level.getBlockState(pos);
        return existing.isAir() || existing.canBeReplaced();
    }

    private static void fail(final @Nullable Player player, final String key, final int arg) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(key, arg).withStyle(ChatFormatting.RED), true);
        }
    }
}
