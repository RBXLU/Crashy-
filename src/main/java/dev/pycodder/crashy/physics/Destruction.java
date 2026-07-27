package dev.pycodder.crashy.physics;

import dev.pycodder.crashy.CrashyConfig;
import dev.pycodder.crashy.registry.CrashySounds;
import dev.pycodder.crashy.settings.CrashySettings;
import dev.pycodder.crashy.settings.CrashySettingsData;
import dev.pycodder.crashy.settings.DestructionMode;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The payoff: a fast-moving object hits the world, and both sides come apart into loose blocks.
 *
 * <p>"Coming apart" means every affected block is re-assembled into its own one-block rigid body —
 * exactly what Sable's {@code /sable assemble shatter} does. Sable hands each new body the momentum
 * of whatever it broke off, so the debris carries the crash forward on its own; on top of that each
 * shard is thrown outwards from the blast centre, which is what makes a crater actually open up
 * instead of the blocks sitting exactly where they were. {@link DebrisManager} later turns the
 * shards back into ordinary world blocks.
 *
 * <p>How much comes apart is up to the world's {@link DestructionMode}.
 */
public final class Destruction {

    /** Chain reactions have to stop somewhere. */
    private static final int MAX_TNT_PER_CRASH = 24;

    /**
     * Crater radius per cube root of energy. Crater volume scaling with energy is roughly how it
     * works in reality, and it happens to give the numbers we want: a single block at launch speed
     * digs about one block, a 400-block build digs the full configured radius.
     */
    private static final double CRATER_PER_ENERGY = 0.09;

    /** Breaking force per cube root of energy. See {@link #impactForce}. */
    private static final double FORCE_PER_ENERGY = 0.6;

    private Destruction() {
    }

    /**
     * A centre things get thrown away from.
     *
     * @param force    how hard this blast hits, measured against block blast resistance
     * @param strength how fast it throws whatever it does break, in m/s
     */
    private record Blast(Vector3d center, double radius, double force, double strength) {
    }

    public static void crash(final ServerLevel level,
                             final ServerSubLevel projectile,
                             final List<BlockPos> hitBlocks,
                             final Vector3d velocity,
                             final int power) {
        final CrashySettingsData settings = CrashySettings.of(level.getServer());

        final double speed = velocity.length();
        final Vector3d impact = averageOf(hitBlocks);
        final Vector3d direction = speed > 1.0e-4 ? velocity.normalize(new Vector3d()) : new Vector3d(0, -1, 0);

        // Nothing breaks, but the hit still happened — the object bounces and you hear it.
        if (settings.mode() == DestructionMode.INDESTRUCTIBLE) {
            playCrashSound(level, impact, speed);
            CrashyEffects.impact(level, impact, 1.5, speed, hitBlocks);
            return;
        }

        // What the object actually brings to the collision. A pebble and a tower travelling at the
        // same speed used to dig identical craters, which is the wrong way round in every respect.
        final double energy = kineticEnergy(projectile, speed);

        final double radius = Mth.clamp(
                CRATER_PER_ENERGY * Math.cbrt(energy),
                0.0,
                CrashyConfig.MAX_DESTRUCTION_RADIUS.get()) * settings.radiusScale();

        if (radius < 0.5) {
            // Not enough behind it to break anything — just the noise and a puff.
            playCrashSound(level, impact, speed);
            CrashyEffects.impact(level, impact, 1.0, speed, hitBlocks);
            return;
        }

        final double scatter = CrashyConfig.DEBRIS_SCATTER.get() * settings.scatterScale();

        final List<Blast> blasts = new ArrayList<>();
        blasts.add(new Blast(impact, radius, impactForce(energy, power), scatter));

        final List<Vector3d> tntCenters = detonateTnt(level, settings, impact, radius, blasts);

        // Both of these queue work rather than doing it: see ShatterQueue for why.
        shatterProjectile(level, settings, projectile, impact, radius, velocity, blasts);

        if (settings.destroyWorld()) {
            shatterWorld(level, settings, blasts, direction, speed);
        }

        CrashyEffects.impact(level, impact, radius, speed, hitBlocks);
        for (final Vector3d tnt : tntCenters) {
            CrashyEffects.tntBlast(level, tnt);
        }

        playCrashSound(level, impact, speed);
    }

    /**
     * Kinetic energy of the projectile, in joules-ish. Sable masses blocks at roughly 1 kg each by
     * default, so this scales with how much of a build is actually arriving.
     */
    private static double kineticEnergy(final ServerSubLevel projectile, final double speed) {
        final double mass = Math.max(1.0, SableBridge.mass(projectile));
        return 0.5 * mass * speed * speed;
    }

    /**
     * How hard the crash hits, in blast-resistance units.
     *
     * <p>Cube-rooted so the numbers stay in the same range as block resistances instead of running
     * away with the mass. At 64 m/s that gives roughly:
     *
     * <ul>
     *     <li>1 block at launch speed — force 12, crater about 1 block</li>
     *     <li>20 blocks — force 25, crater about 3</li>
     *     <li>400 blocks — force 60, crater at the configured maximum</li>
     * </ul>
     *
     * <p>Obsidian (resistance 1200) survives all of it, which is the intent.
     */
    private static double impactForce(final double energy, final int power) {
        return FORCE_PER_ENERGY * Math.cbrt(energy) + power * 0.5;
    }

    /**
     * @return true if this blast can actually break the block, given the current mode
     */
    private static boolean canBreak(final CrashySettingsData settings,
                                    final BlockState state,
                                    final double forceHere) {
        if (settings.mode() != DestructionMode.REALISTIC) {
            return true;
        }
        final double resistance = state.getBlock().getExplosionResistance() * settings.toughnessScale();
        return forceHere >= resistance;
    }

    // ------------------------------------------------------------------ TNT

    /**
     * Turns any TNT caught in the crash into an extra, much stronger blast centre.
     *
     * <p>The TNT block is removed by hand rather than primed, because a vanilla explosion would
     * delete the surrounding blocks outright. The whole point here is that they survive and fly.
     */
    private static List<Vector3d> detonateTnt(final ServerLevel level,
                                              final CrashySettingsData settings,
                                              final Vector3d impact,
                                              final double radius,
                                              final List<Blast> blasts) {
        final List<Vector3d> centers = new ArrayList<>();
        if (!settings.tntBlasts()) {
            return centers;
        }

        final double blastRadius = CrashyConfig.TNT_BLAST_RADIUS.get();
        final double blastStrength = CrashyConfig.TNT_BLAST_STRENGTH.get() * settings.scatterScale();
        final double blastForce = CrashyConfig.TNT_BLAST_FORCE.get();

        final List<Vector3d> frontier = new ArrayList<>();
        frontier.add(impact);
        double searchRadius = radius * 1.6;

        final Set<BlockPos> consumed = new HashSet<>();

        while (!frontier.isEmpty() && consumed.size() < MAX_TNT_PER_CRASH) {
            final Vector3d center = frontier.removeFirst();

            for (final BlockPos pos : positionsInSphere(level, center, searchRadius)) {
                if (consumed.size() >= MAX_TNT_PER_CRASH || consumed.contains(pos)) {
                    continue;
                }
                if (!level.getBlockState(pos).is(Blocks.TNT)) {
                    continue;
                }

                consumed.add(pos);
                level.removeBlock(pos, false);

                final Vector3d tntCenter = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                centers.add(tntCenter);
                blasts.add(new Blast(tntCenter, blastRadius, blastForce, blastStrength));

                // Neighbouring TNT should go up too.
                frontier.add(tntCenter);
            }

            // Chained charges only look for each other nearby.
            searchRadius = blastRadius;
        }

        return centers;
    }

    // ------------------------------------------------------------------ the object

    private static void shatterProjectile(final ServerLevel level,
                                          final CrashySettingsData settings,
                                          final ServerSubLevel projectile,
                                          final Vector3d impact,
                                          final double radius,
                                          final Vector3d velocity,
                                          final List<Blast> blasts) {
        final BoundingBox3ic bounds = SableBridge.plotBounds(projectile);
        if (bounds == null) {
            return;
        }

        // The impact point, expressed in the object's own plot coordinates.
        final Vector3d localImpact = projectile.logicalPose().transformPositionInverse(new Vector3d(impact));

        // Scanning the whole plot is fine for a normal build, but a 100-block-wide contraption would
        // be a million block lookups in one tick, so huge objects are only scanned near the impact.
        final int scanRadius = (int) Math.ceil(radius * 1.6) + 1;
        final long volume = (long) (bounds.maxX() - bounds.minX() + 1)
                * (bounds.maxY() - bounds.minY() + 1)
                * (bounds.maxZ() - bounds.minZ() + 1);

        final int minX, minY, minZ, maxX, maxY, maxZ;
        if (volume <= 64_000) {
            minX = bounds.minX();
            minY = bounds.minY();
            minZ = bounds.minZ();
            maxX = bounds.maxX();
            maxY = bounds.maxY();
            maxZ = bounds.maxZ();
        } else {
            minX = Math.max(bounds.minX(), Mth.floor(localImpact.x) - scanRadius);
            minY = Math.max(bounds.minY(), Mth.floor(localImpact.y) - scanRadius);
            minZ = Math.max(bounds.minZ(), Mth.floor(localImpact.z) - scanRadius);
            maxX = Math.min(bounds.maxX(), Mth.floor(localImpact.x) + scanRadius);
            maxY = Math.min(bounds.maxY(), Mth.floor(localImpact.y) + scanRadius);
            maxZ = Math.min(bounds.maxZ(), Mth.floor(localImpact.z) + scanRadius);
        }

        final List<BlockPos> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        blocks.add(pos);
                    }
                }
            }
        }

        final int cap = CrashyConfig.MAX_DEBRIS_PER_IMPACT.get();
        if (blocks.size() > cap) {
            // Too big to disintegrate wholesale — only the region that actually took the hit breaks
            // off, which also happens to look far better than a skyscraper exploding into confetti.
            final double localRadiusSq = (radius * 1.6) * (radius * 1.6);
            blocks.removeIf(pos -> distanceSq(pos, localImpact) > localRadiusSq);
            if (blocks.size() > cap) {
                blocks.sort(Comparator.comparingDouble(pos -> distanceSq(pos, localImpact)));
                blocks.subList(cap, blocks.size()).clear();
            }
        }

        final RandomSource random = level.random;
        for (final BlockPos plotPos : blocks) {
            // Where this block is in the world right now, so blasts push it the right way.
            final Vector3d worldPos = SableBridge.plotToWorld(projectile, plotPos, new Vector3d());

            if (!canBreak(settings, level.getBlockState(plotPos), forceAt(blasts, worldPos))) {
                continue;
            }

            // Sable gives each shard the object's velocity when it splits off, so this is only the
            // bounce-back on top of it.
            final Vector3d push = blastPush(blasts, worldPos)
                    .fma(-0.3, velocity)
                    .add(jitter(random, 2.0));
            ShatterQueue.enqueue(level, plotPos, push, spin(random), true);
        }
    }

    // ------------------------------------------------------------------ the world

    private static void shatterWorld(final ServerLevel level,
                                     final CrashySettingsData settings,
                                     final List<Blast> blasts,
                                     final Vector3d direction,
                                     final double speed) {
        final int limit = CrashyConfig.MAX_WORLD_BLOCKS_DESTROYED.get();
        if (limit <= 0) {
            return;
        }

        final Vector3d primary = blasts.getFirst().center();
        final Set<BlockPos> seen = new HashSet<>();
        final List<BlockPos> candidates = new ArrayList<>();

        for (final Blast blast : blasts) {
            for (final BlockPos pos : positionsInSphere(level, blast.center(), blast.radius())) {
                if (!seen.add(pos)) {
                    continue;
                }
                if (SableBridge.subLevelAt(level, pos) != null) {
                    continue; // already part of somebody else's physics object
                }

                final BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.liquid()) {
                    continue;
                }
                if (state.getDestroySpeed(level, pos) < 0.0f) {
                    continue; // bedrock and friends stay put
                }

                final Vector3d worldPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (!canBreak(settings, state, forceAt(blasts, worldPos))) {
                    continue;
                }
                candidates.add(pos);
            }
        }

        candidates.sort(Comparator.comparingDouble(pos -> distanceSq(pos, primary)));
        if (candidates.size() > limit) {
            candidates.subList(limit, candidates.size()).clear();
        }

        final RandomSource random = level.random;
        final Vector3d forward = direction.mul(Math.min(speed * 0.30, 16.0), new Vector3d());

        for (final BlockPos pos : candidates) {
            final BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            // Flimsy things (torches, grass, panes) just break the vanilla way.
            if (state.getCollisionShape(level, pos).isEmpty()) {
                level.destroyBlock(pos, true);
                continue;
            }

            final Vector3d worldPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

            final Vector3d push = blastPush(blasts, worldPos)
                    .add(forward)
                    .add(jitter(random, 2.5));
            ShatterQueue.enqueue(level, pos, push, spin(random), false);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Strongest blast force reaching this point, after distance falloff. */
    private static double forceAt(final List<Blast> blasts, final Vector3d worldPos) {
        double best = 0.0;
        for (final Blast blast : blasts) {
            final double distance = worldPos.distance(blast.center());
            if (distance > blast.radius() + 1.0) {
                continue;
            }
            best = Math.max(best, blast.force() * (1.0 - distance / (blast.radius() + 1.0)));
        }
        return best;
    }

    /**
     * Combined outward shove from every blast centre.
     *
     * <p>This is the difference between a crater and a block that quietly settles back where it
     * was: without a real velocity a shard stops moving immediately and {@link DebrisManager} puts
     * it straight back, which looks like the destruction undoing itself.
     */
    private static Vector3d blastPush(final List<Blast> blasts, final Vector3d worldPos) {
        final Vector3d total = new Vector3d();

        for (final Blast blast : blasts) {
            final Vector3d away = worldPos.sub(blast.center(), new Vector3d());
            final double distance = away.length();

            if (distance < 1.0e-3) {
                // Dead centre: pick an upward-ish direction rather than dividing by zero.
                total.add(0.0, blast.strength(), 0.0);
                continue;
            }

            // Linear falloff, never dropping to nothing inside the blast.
            final double falloff = Math.max(0.25, 1.0 - distance / (blast.radius() + 1.0));
            total.fma(blast.strength() * falloff / distance, away);
        }

        // Lift, scaled to the blast rather than fixed, so ground debris actually hops clear of the
        // crater instead of grinding along the floor and settling straight back into the hole.
        double strongest = 0.0;
        for (final Blast blast : blasts) {
            strongest = Math.max(strongest, blast.strength());
        }
        total.y += 1.5 + strongest * 0.2;
        return total;
    }

    private static Iterable<BlockPos> positionsInSphere(final ServerLevel level,
                                                        final Vector3d center,
                                                        final double radius) {
        final List<BlockPos> result = new ArrayList<>();
        final double radiusSq = radius * radius;

        for (int x = Mth.floor(center.x - radius); x <= Mth.floor(center.x + radius); x++) {
            for (int y = Mth.floor(center.y - radius); y <= Mth.floor(center.y + radius); y++) {
                for (int z = Mth.floor(center.z - radius); z <= Mth.floor(center.z + radius); z++) {
                    final BlockPos pos = new BlockPos(x, y, z);
                    if (distanceSq(pos, center) <= radiusSq && level.isLoaded(pos)) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }

    private static void playCrashSound(final ServerLevel level, final Vector3d impact, final double speed) {
        level.playSound(null, impact.x, impact.y, impact.z,
                CrashySounds.IMPACT.get(), SoundSource.BLOCKS,
                Math.min(4.0F, 1.1F + (float) speed * 0.05F),
                0.82F + level.random.nextFloat() * 0.25F);
    }

    private static Vector3d jitter(final RandomSource random, final double amount) {
        return new Vector3d(
                (random.nextDouble() - 0.5) * amount,
                (random.nextDouble() - 0.2) * amount,
                (random.nextDouble() - 0.5) * amount);
    }

    private static Vector3d spin(final RandomSource random) {
        return new Vector3d(
                (random.nextDouble() - 0.5) * 8.0,
                (random.nextDouble() - 0.5) * 8.0,
                (random.nextDouble() - 0.5) * 8.0);
    }

    private static double distanceSq(final BlockPos pos, final Vector3d point) {
        final double dx = pos.getX() + 0.5 - point.x;
        final double dy = pos.getY() + 0.5 - point.y;
        final double dz = pos.getZ() + 0.5 - point.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static Vector3d averageOf(final List<BlockPos> positions) {
        final Vector3d sum = new Vector3d();
        for (final BlockPos pos : positions) {
            sum.add(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
        return positions.isEmpty() ? sum : sum.div(positions.size());
    }
}
