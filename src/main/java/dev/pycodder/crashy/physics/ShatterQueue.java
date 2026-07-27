package dev.pycodder.crashy.physics;

import dev.pycodder.crashy.CrashyConfig;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Breaks blocks into individual physics bodies a few at a time.
 *
 * <p>Turning one block into its own rigid body means allocating a Sable plot, a chunk to hold it
 * and an entry in the physics pipeline. That is fine for a handful. Doing it for every block of a
 * 400-block build plus its crater — well over five hundred bodies — inside a single tick allocates
 * hundreds of chunks at once and takes the server out, which is exactly what used to happen.
 *
 * <p>So the work is queued instead. Each tick a fixed budget of blocks is converted, and once the
 * world is carrying its configured maximum of live debris the rest of the queue is dropped: the
 * crater comes out smaller under load rather than the game dying. Nothing is deleted — a block that
 * never gets shattered simply stays where it is.
 */
public final class ShatterQueue {

    private static final Deque<Job> QUEUE = new ArrayDeque<>();

    private ShatterQueue() {
    }

    /**
     * @param plotSpace true when the position is inside a sub-level's plot rather than the world;
     *                  those are the projectile's own blocks and must not be skipped for being
     *                  "already part of a physics object"
     */
    private record Job(ResourceKey<Level> dimension,
                       BlockPos pos,
                       Vector3d push,
                       Vector3d spin,
                       boolean plotSpace) {
    }

    /** Queues one block. The velocity is applied to the shard once it actually exists. */
    public static void enqueue(final ServerLevel level,
                               final BlockPos pos,
                               final Vector3d push,
                               final Vector3d spin,
                               final boolean plotSpace) {
        QUEUE.add(new Job(level.dimension(), pos.immutable(), new Vector3d(push), new Vector3d(spin), plotSpace));
    }

    /** How many blocks are still waiting. Used by the debug command and the tests. */
    public static int pending() {
        return QUEUE.size();
    }

    public static void clear() {
        QUEUE.clear();
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (QUEUE.isEmpty()) {
            return;
        }

        final MinecraftServer server = event.getServer();
        final int budget = CrashyConfig.SHATTERS_PER_TICK.get();
        final int maxLive = CrashyConfig.MAX_LIVE_DEBRIS.get();

        int done = 0;

        while (done < budget && !QUEUE.isEmpty()) {
            // Over budget for the world as a whole: abandon the rest rather than pile on.
            if (DebrisManager.liveCount() >= maxLive) {
                QUEUE.clear();
                break;
            }

            final Job job = QUEUE.poll();
            final ServerLevel level = server.getLevel(job.dimension());
            if (level == null) {
                continue;
            }

            if (!level.isLoaded(job.pos()) || level.getBlockState(job.pos()).isAir()) {
                continue; // something else got to it while the job was waiting
            }
            if (!job.plotSpace() && SableBridge.subLevelAt(level, job.pos()) != null) {
                continue; // already became part of somebody's physics object
            }

            final ServerSubLevel shard = SableBridge.shatterSingleBlock(level, job.pos());
            if (shard == null) {
                continue;
            }

            SableBridge.addVelocity(shard, job.push(), job.spin());
            // Tracked against its own level: a queue can hold jobs from several dimensions.
            DebrisManager.track(level, List.of(shard));
            done++;
        }
    }
}
