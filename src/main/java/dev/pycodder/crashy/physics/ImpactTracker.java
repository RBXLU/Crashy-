package dev.pycodder.crashy.physics;

import dev.pycodder.crashy.CrashyConfig;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Watches objects that were fired by the launcher and decides when they have crashed into
 * something.
 *
 * <p>Sable resolves the collision itself, but it does not tell us about it, so we detect the hit by
 * sweeping the body's world-space bounding box forward along its velocity and looking for solid
 * world blocks in the way. That deliberately reads ordinary world coordinates — a sub-level's own
 * blocks live inside a far-away plot, so they can never be mistaken for the terrain it is about to
 * hit.
 */
public final class ImpactTracker {

    private static final int MAX_TRACK_TICKS = 600;

    /**
     * Breathing room between two impacts by the same body, so ploughing through a floor registers
     * as one crash rather than one per tick of contact.
     */
    private static final int TICKS_BETWEEN_IMPACTS = 6;
    private static final int MAX_SCANNED_POSITIONS = 24_000;

    private static final List<Tracked> TRACKED = new ArrayList<>();

    private ImpactTracker() {
    }

    private static final class Tracked {
        private final ServerSubLevel subLevel;
        private final ResourceKey<Level> dimension;
        private final int power;
        private final Vector3d launchPosition;
        private int age;
        /** Ticks left before this body may hit something again. */
        private int cooldown;

        private Tracked(final ServerSubLevel subLevel, final ResourceKey<Level> dimension, final int power) {
            this.subLevel = subLevel;
            this.dimension = dimension;
            this.power = power;
            this.launchPosition = SableBridge.position(subLevel, new Vector3d());
        }
    }

    public static void track(final ServerLevel level, final ServerSubLevel subLevel, final int power) {
        untrack(subLevel);
        TRACKED.add(new Tracked(subLevel, level.dimension(), power));
    }

    public static void untrack(final ServerSubLevel subLevel) {
        TRACKED.removeIf(tracked -> tracked.subLevel == subLevel);
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (TRACKED.isEmpty()) {
            return;
        }

        final MinecraftServer server = event.getServer();
        final Iterator<Tracked> iterator = TRACKED.iterator();

        while (iterator.hasNext()) {
            final Tracked tracked = iterator.next();

            if (tracked.subLevel.isRemoved() || ++tracked.age > MAX_TRACK_TICKS) {
                iterator.remove();
                continue;
            }

            final ServerLevel level = server.getLevel(tracked.dimension);
            if (level == null) {
                iterator.remove();
                continue;
            }

            final Vector3d velocity = SableBridge.linearVelocity(tracked.subLevel, new Vector3d());
            final double speed = velocity.length();

            if (speed < CrashyConfig.MIN_IMPACT_SPEED.get()) {
                // It never hit anything hard enough; let it be an ordinary physics object again.
                if (tracked.age > 40) {
                    iterator.remove();
                }
                continue;
            }

            final Vector3d position = SableBridge.position(tracked.subLevel, new Vector3d());

            if (tracked.age % 2 == 0) {
                CrashyEffects.trail(level, position, speed);
            }

            if (tracked.cooldown > 0) {
                tracked.cooldown--;
                continue;
            }

            if (tracked.age < 2 || position.distance(tracked.launchPosition) < 1.5) {
                continue;
            }

            final List<BlockPos> hits = findWorldBlocksInPath(level, tracked.subLevel, position, velocity, speed);
            if (hits.isEmpty()) {
                continue;
            }

            Destruction.crash(level, tracked.subLevel, hits, velocity, tracked.power);

            // Something big enough to punch through the ground keeps going, and keeps breaking what
            // it meets. Only what is actually gone stops being tracked.
            if (tracked.subLevel.isRemoved()) {
                iterator.remove();
                continue;
            }

            tracked.cooldown = TICKS_BETWEEN_IMPACTS;
            tracked.launchPosition.set(position);
        }
    }

    /**
     * @return solid world blocks that the body is about to plough into, or an empty list.
     */
    private static List<BlockPos> findWorldBlocksInPath(final ServerLevel level,
                                                        final ServerSubLevel subLevel,
                                                        final Vector3d position,
                                                        final Vector3d velocity,
                                                        final double speed) {
        final BoundingBox3dc bounds = subLevel.boundingBox();
        final Vector3d direction = velocity.normalize(new Vector3d());

        // Look a fraction of a second ahead so we catch the wall on the tick before it is hit.
        final double lead = Math.min(speed * 0.12, 4.0);

        final int minX = Mth.floor(Math.min(bounds.minX(), bounds.minX() + direction.x * lead));
        final int minY = Mth.floor(Math.min(bounds.minY(), bounds.minY() + direction.y * lead));
        final int minZ = Mth.floor(Math.min(bounds.minZ(), bounds.minZ() + direction.z * lead));
        final int maxX = Mth.floor(Math.max(bounds.maxX(), bounds.maxX() + direction.x * lead));
        final int maxY = Mth.floor(Math.max(bounds.maxY(), bounds.maxY() + direction.y * lead));
        final int maxZ = Mth.floor(Math.max(bounds.maxZ(), bounds.maxZ() + direction.z * lead));

        final long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > MAX_SCANNED_POSITIONS) {
            return List.of();
        }

        final List<BlockPos> hits = new ArrayList<>();
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }

                    final BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || state.liquid()) {
                        continue;
                    }
                    if (state.getCollisionShape(level, cursor).isEmpty()) {
                        continue;
                    }

                    // Only the leading face counts, otherwise trailing terrain triggers a false crash.
                    final double aheadX = x + 0.5 - position.x;
                    final double aheadY = y + 0.5 - position.y;
                    final double aheadZ = z + 0.5 - position.z;
                    if (aheadX * direction.x + aheadY * direction.y + aheadZ * direction.z <= 0.0) {
                        continue;
                    }

                    hits.add(cursor.immutable());
                }
            }
        }

        return hits;
    }
}
