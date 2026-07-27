package dev.pycodder.crashy.physics;

import dev.pycodder.crashy.CrashyConfig;
import dev.pycodder.crashy.settings.CrashySettings;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Puts the world back together.
 *
 * <p>Every shard produced by {@link Destruction} is a live rigid body. Left alone they would pile up
 * until the physics pipeline drowns, so once a shard has come to rest it is written back into the
 * world as an ordinary block and its body is deleted.
 */
public final class DebrisManager {

    private static final double RESTING_SPEED = 0.35;

    /**
     * A shard is not allowed to settle before this many ticks, however still it looks.
     *
     * <p>Without it, a block that was knocked loose but wedged in place would settle almost
     * immediately, right back where it started — which reads as the destruction undoing itself a
     * second after the crash.
     */
    private static final int MIN_AGE_BEFORE_SETTLING = 45;

    private static final List<Debris> DEBRIS = new ArrayList<>();

    private DebrisManager() {
    }

    private static final class Debris {
        private final ServerSubLevel subLevel;
        private final ResourceKey<Level> dimension;
        private int age;
        private int restingTicks;

        private Debris(final ServerSubLevel subLevel, final ResourceKey<Level> dimension) {
            this.subLevel = subLevel;
            this.dimension = dimension;
        }
    }

    public static void track(final ServerLevel level, final Collection<ServerSubLevel> shards) {
        for (final ServerSubLevel shard : shards) {
            DEBRIS.add(new Debris(shard, level.dimension()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (DEBRIS.isEmpty()) {
            return;
        }

        final MinecraftServer server = event.getServer();

        final boolean settle = CrashySettings.of(server).settleDebris();
        final int settleDelay = CrashyConfig.DEBRIS_SETTLE_DELAY.get();
        final int maxLifetime = CrashyConfig.DEBRIS_MAX_LIFETIME.get();

        final Iterator<Debris> iterator = DEBRIS.iterator();

        while (iterator.hasNext()) {
            final Debris debris = iterator.next();

            if (debris.subLevel.isRemoved()) {
                iterator.remove();
                continue;
            }

            debris.age++;

            if (!settle) {
                if (debris.age > maxLifetime) {
                    iterator.remove();
                }
                continue;
            }

            final ServerLevel level = server.getLevel(debris.dimension);
            if (level == null) {
                iterator.remove();
                continue;
            }

            final double speed = SableBridge.linearVelocity(debris.subLevel, new Vector3d()).length();
            debris.restingTicks = speed < RESTING_SPEED ? debris.restingTicks + 1 : 0;

            final boolean settled = debris.age >= MIN_AGE_BEFORE_SETTLING && debris.restingTicks >= settleDelay;
            if (settled || debris.age >= maxLifetime) {
                iterator.remove();
                settle(level, debris.subLevel);
            }
        }
    }

    /** Writes a one-block shard back into the world and deletes its rigid body. */
    private static void settle(final ServerLevel level, final ServerSubLevel shard) {
        final BlockState state = findBlockState(level, shard);
        final Vector3d position = SableBridge.position(shard, new Vector3d());
        final BlockPos preferred = BlockPos.containing(position.x, position.y, position.z);

        // Clears the plot without dropping anything — this method owns what happens to the block.
        SableBridge.destroy(level, shard);

        if (state == null || state.isAir()) {
            return;
        }

        final BlockPos target = findFreeSpot(level, preferred);
        if (target != null) {
            level.setBlockAndUpdate(target, state);
            return;
        }

        // Nowhere to put it — hand the player the item instead of quietly deleting the block.
        Block.popResource(level, preferred, new ItemStack(state.getBlock()));
    }

    private static @Nullable BlockState findBlockState(final ServerLevel level, final ServerSubLevel shard) {
        for (final BlockPos pos : SableBridge.blocksInPlot(level, shard)) {
            return level.getBlockState(pos);
        }
        return null;
    }

    private static @Nullable BlockPos findFreeSpot(final ServerLevel level, final BlockPos preferred) {
        if (isFree(level, preferred)) {
            return preferred;
        }
        for (final Direction direction : Direction.values()) {
            final BlockPos candidate = preferred.relative(direction);
            if (isFree(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isFree(final ServerLevel level, final BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
            return false;
        }
        // Never drop a shard inside somebody's plot — that space belongs to Sable.
        if (SableBridge.subLevelAt(level, pos) != null) {
            return false;
        }
        final BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }
}
