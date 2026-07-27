package dev.pycodder.crashy.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.List;

/** All the particle work, kept out of the gameplay code. */
public final class CrashyEffects {

    private CrashyEffects() {
    }

    /** The crash: flash, fireball, a ring of dust kicked outwards, and shrapnel of the real blocks. */
    public static void impact(final ServerLevel level,
                              final Vector3d center,
                              final double radius,
                              final double speed,
                              final List<BlockPos> hitBlocks) {
        final RandomSource random = level.random;

        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);

        // Shrapnel that matches whatever was actually hit.
        for (final BlockPos pos : hitBlocks.subList(0, Math.min(hitBlocks.size(), 8))) {
            final BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    28, 0.45, 0.45, 0.45, 0.6);
        }

        // Dust ring hugging the ground — reads as a shockwave rather than a puff.
        final int ringPoints = Mth.clamp((int) (radius * 14.0), 12, 56);
        for (int i = 0; i < ringPoints; i++) {
            final double angle = (Math.PI * 2.0 * i) / ringPoints;
            final double r = radius * (0.85 + random.nextDouble() * 0.5);
            final double dx = Math.cos(angle);
            final double dz = Math.sin(angle);

            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    center.x + dx * r, center.y - radius * 0.25, center.z + dz * r,
                    0, dx * 0.55, 0.06, dz * 0.55, 0.65);
        }

        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z,
                (int) Math.min(70, 14 + radius * 12), radius * 0.5, radius * 0.4, radius * 0.5, 0.06);

        // Sparks scale with how hard the hit was.
        final int sparks = Mth.clamp((int) (speed * 1.6), 6, 60);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
                sparks, radius * 0.4, radius * 0.4, radius * 0.4, 0.75);
    }

    /** A charge going off inside the crash: fire and lava spatter on top of the normal blast. */
    public static void tntBlast(final ServerLevel level, final Vector3d center) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 45, 0.7, 0.7, 0.7, 0.35);
        level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 14, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 50, 1.1, 1.1, 1.1, 0.12);

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 3.4F, 0.62F);
    }

    /** Faint smoke trailing a launched object so you can follow it across the sky. */
    public static void trail(final ServerLevel level, final Vector3d position, final double speed) {
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                position.x, position.y, position.z, 2, 0.25, 0.25, 0.25, 0.0);
        if (speed > 24.0) {
            level.sendParticles(ParticleTypes.CRIT,
                    position.x, position.y, position.z, 2, 0.3, 0.3, 0.3, 0.05);
        }
    }

    /** Glue setting on a block. */
    public static void glue(final ServerLevel level, final BlockPos pos) {
        level.sendParticles(ParticleTypes.WAX_ON,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                10, 0.42, 0.42, 0.42, 0.0);
    }

    public static void unglue(final ServerLevel level, final BlockPos pos) {
        level.sendParticles(ParticleTypes.WAX_OFF,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                14, 0.5, 0.5, 0.5, 0.02);
    }

    /** Physics coming online: a rising column plus a burst at the base. */
    public static void activate(final ServerLevel level, final BlockPos pos) {
        final double cx = pos.getX() + 0.5;
        final double cy = pos.getY() + 0.5;
        final double cz = pos.getZ() + 0.5;

        level.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 1, 0, 0, 0, 0);

        for (int i = 0; i < 26; i++) {
            final double angle = i * 0.7;
            final double height = i * 0.11;
            level.sendParticles(ParticleTypes.END_ROD,
                    cx + Math.cos(angle) * 0.75, cy + height, cz + Math.sin(angle) * 0.75,
                    0, Math.cos(angle) * 0.05, 0.22, Math.sin(angle) * 0.05, 0.5);
        }

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 24, 0.55, 0.55, 0.55, 0.35);
    }

    /** Physics being switched back off and the build settling into the world. */
    public static void deactivate(final ServerLevel level, final Vector3d center) {
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 30, 0.9, 0.9, 0.9, 0.03);
        level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 1.0, center.z, 40, 1.0, 1.0, 1.0, 0.4);
    }

    /** A structure appearing from a blueprint. */
    public static void blueprint(final ServerLevel level, final Vector3d center, final double radius) {
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                (int) Mth.clamp(radius * 10.0, 16, 90), radius, radius * 0.6, radius, 0.06);
        level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z,
                (int) Mth.clamp(radius * 8.0, 12, 70), radius, radius * 0.6, radius, 0.5);
    }
}
