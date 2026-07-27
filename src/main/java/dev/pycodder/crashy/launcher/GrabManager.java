package dev.pycodder.crashy.launcher;

import dev.pycodder.crashy.CrashyConfig;
import dev.pycodder.crashy.physics.ImpactTracker;
import dev.pycodder.crashy.physics.SableBridge;
import dev.pycodder.crashy.registry.CrashyComponents;
import dev.pycodder.crashy.registry.CrashyItems;
import dev.pycodder.crashy.registry.CrashySounds;
import dev.pycodder.crashy.settings.CrashySettings;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side state for "the launcher is currently holding this object".
 *
 * <p>A held body is driven by overwriting its velocity every tick so it chases a point in front of
 * the player's eyes — the classic gravity-gun feel, and it keeps Sable's solver in charge of
 * collisions along the way.
 */
public final class GrabManager {

    /** Maximum charge level, as promised on the tin. */
    public static final int MAX_POWER = 10;

    private static final Map<UUID, Grab> GRABS = new HashMap<>();

    private GrabManager() {
    }

    private static final class Grab {
        private final ServerSubLevel subLevel;
        private final ResourceKey<Level> dimension;
        private final double distance;

        private Grab(final ServerSubLevel subLevel, final ResourceKey<Level> dimension, final double distance) {
            this.subLevel = subLevel;
            this.dimension = dimension;
            this.distance = distance;
        }
    }

    // ------------------------------------------------------------------ public API

    public static boolean isGrabbing(final ServerPlayer player) {
        return GRABS.containsKey(player.getUUID());
    }

    public static @Nullable ServerSubLevel grabbed(final ServerPlayer player) {
        final Grab grab = GRABS.get(player.getUUID());
        return grab == null || grab.subLevel.isRemoved() ? null : grab.subLevel;
    }

    public static boolean grab(final ServerPlayer player, final ServerSubLevel subLevel, final InteractionHand hand) {
        if (subLevel.isRemoved()) {
            return false;
        }

        final Vector3d position = SableBridge.position(subLevel, new Vector3d());
        final double distance = Math.min(
                player.getEyePosition().distanceTo(new Vec3(position.x, position.y, position.z)),
                CrashyConfig.HOLD_DISTANCE.get());

        GRABS.put(player.getUUID(), new Grab(subLevel, player.level().dimension(), Math.max(2.0, distance)));
        ImpactTracker.untrack(subLevel);
        markStack(player.getItemInHand(hand), true);

        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.PLAYERS, 0.8F, 0.7F);
        player.displayClientMessage(Component.translatable("message.crashy.launcher.grabbed",
                (int) Math.round(SableBridge.mass(subLevel))).withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    public static void release(final ServerPlayer player) {
        if (GRABS.remove(player.getUUID()) == null) {
            return;
        }
        clearMarkers(player);
        player.stopUsingItem();
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ALLAY_THROW, SoundSource.PLAYERS, 0.6F, 0.7F);
        player.displayClientMessage(Component.translatable("message.crashy.launcher.released")
                .withStyle(ChatFormatting.GRAY), true);
    }

    /** Fires the held object along the player's line of sight. */
    public static void launch(final ServerPlayer player, final int power) {
        final Grab grab = GRABS.remove(player.getUUID());
        clearMarkers(player);
        if (grab == null || grab.subLevel.isRemoved()) {
            return;
        }

        final ServerLevel level = player.serverLevel();
        final Vec3 look = player.getLookAngle().normalize();
        final double speed = power * CrashySettings.of(level.getServer()).speedPerPower();

        final Vector3d linear = new Vector3d(look.x * speed, look.y * speed, look.z * speed);
        // A touch of spin so the object tumbles instead of sliding through the air like a brick.
        final Vector3d angular = new Vector3d(
                (level.random.nextDouble() - 0.5) * power * 0.25,
                (level.random.nextDouble() - 0.5) * power * 0.25,
                (level.random.nextDouble() - 0.5) * power * 0.25);

        SableBridge.setVelocity(grab.subLevel, linear, angular);
        ImpactTracker.track(level, grab.subLevel, power);

        final Vector3d position = SableBridge.position(grab.subLevel, new Vector3d());
        level.playSound(null, position.x, position.y, position.z,
                CrashySounds.LAUNCH.get(), SoundSource.PLAYERS,
                0.7F + power * 0.06F, 1.35F - power * 0.055F);
        level.sendParticles(ParticleTypes.CLOUD, position.x, position.y, position.z,
                6 + power * 2, 0.5, 0.5, 0.5, 0.08);

        player.displayClientMessage(Component.translatable("message.crashy.launcher.launched", power)
                .withStyle(ChatFormatting.GOLD), true);
    }

    /** Charge level from how long right-click has been held. */
    public static int powerFor(final int useTicks) {
        final int perPower = CrashyConfig.TICKS_PER_POWER.get();
        return Math.max(1, Math.min(MAX_POWER, useTicks / perPower));
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (GRABS.isEmpty()) {
            return;
        }

        final MinecraftServer server = event.getServer();
        final Iterator<Map.Entry<UUID, Grab>> iterator = GRABS.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<UUID, Grab> entry = iterator.next();
            final ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

            if (player == null) {
                iterator.remove();
                continue;
            }
            if (!hold(player, entry.getValue())) {
                iterator.remove();
                clearMarkers(player);
                player.stopUsingItem();
            }
        }
    }

    /** @return false when the grab should be dropped. */
    private static boolean hold(final ServerPlayer player, final Grab grab) {
        if (player.isRemoved() || player.isDeadOrDying()) {
            return false;
        }
        if (!player.level().dimension().equals(grab.dimension)) {
            return false;
        }
        if (holdingHand(player) == null) {
            return false;
        }
        // Same reason as in ImpactTracker: no mass means no centre of mass, and driving its
        // velocity would hand Rapier a body it cannot step.
        if (grab.subLevel.isRemoved() || !SableBridge.hasMass(grab.subLevel)) {
            return false;
        }

        final Vec3 eye = player.getEyePosition();
        final Vec3 look = player.getLookAngle();
        final Vector3d target = new Vector3d(
                eye.x + look.x * grab.distance,
                eye.y + look.y * grab.distance,
                eye.z + look.z * grab.distance);

        final Vector3d current = SableBridge.position(grab.subLevel, new Vector3d());
        final Vector3d delta = target.sub(current, new Vector3d());
        final double distance = delta.length();

        if (distance > CrashyConfig.MAX_HOLD_DISTANCE.get()) {
            return false;
        }

        // Proportional controller, capped so a heavy build cannot be slingshotted by yanking the mouse.
        final Vector3d desired = delta.mul(6.0, new Vector3d());
        final double speed = desired.length();
        if (speed > 24.0) {
            desired.mul(24.0 / speed);
        }

        // Bleed off tumbling so the player can actually aim the thing.
        final Vector3d angular = SableBridge.angularVelocity(grab.subLevel, new Vector3d()).mul(0.75);

        SableBridge.setVelocity(grab.subLevel, desired, angular);
        return true;
    }

    /** The hand currently holding a launcher, or null. */
    public static @Nullable InteractionHand holdingHand(final ServerPlayer player) {
        if (player.getMainHandItem().is(CrashyItems.LAUNCHER.get())) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(CrashyItems.LAUNCHER.get())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static void markStack(final ItemStack stack, final boolean grabbing) {
        if (grabbing) {
            stack.set(CrashyComponents.GRABBING.get(), true);
        } else {
            stack.remove(CrashyComponents.GRABBING.get());
        }
    }

    private static void clearMarkers(final ServerPlayer player) {
        markStack(player.getMainHandItem(), false);
        markStack(player.getOffhandItem(), false);
    }
}
