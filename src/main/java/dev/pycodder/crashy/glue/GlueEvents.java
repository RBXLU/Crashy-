package dev.pycodder.crashy.glue;

import dev.pycodder.crashy.network.GlueHighlightPayload;
import dev.pycodder.crashy.physics.CrashyEffects;
import dev.pycodder.crashy.registry.CrashyItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Hooks that keep {@link GlueSavedData} in sync with what actually happens in the world. */
public final class GlueEvents {

    private static final int HIGHLIGHT_INTERVAL_TICKS = 10;
    private static final int HIGHLIGHT_RADIUS = 28;

    private GlueEvents() {
    }

    /**
     * The headline feature: with glue in one hand, every block placed with the other hand is glued
     * to the structure automatically.
     */
    @SubscribeEvent
    public static void onBlockPlaced(final BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof final ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        final InteractionHand glueHand = findGlueHand(player);
        if (glueHand == null) {
            return;
        }

        final BlockPos pos = event.getPos();
        if (level.getBlockState(pos).isAir()) {
            return;
        }

        final GlueSavedData glue = GlueSavedData.get(level);
        if (!glue.glue(pos)) {
            player.displayClientMessage(Component.translatable("message.crashy.glue.limit")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        showGlueEffect(level, pos);
        player.displayClientMessage(Component.translatable("message.crashy.glue.auto", glue.getGroupSize(pos))
                .withStyle(ChatFormatting.AQUA), true);

        if (!player.getAbilities().instabuild) {
            final ItemStack stack = player.getItemInHand(glueHand);
            stack.hurtAndBreak(1, player, glueHand == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND);
        }
    }

    /**
     * Feeds the client-side highlight. Only players actually holding glue get the packet, and only
     * twice a second — the outline does not need to be frame-accurate.
     */
    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % HIGHLIGHT_INTERVAL_TICKS != 0) {
            return;
        }

        for (final ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (findGlueHand(player) == null) {
                continue; // the client expires its own data, so no "stop" packet is needed
            }

            final List<BlockPos> positions = GlueSavedData.get(player.serverLevel())
                    .near(player.blockPosition(), HIGHLIGHT_RADIUS, GlueHighlightPayload.MAX_POSITIONS);

            PacketDistributor.sendToPlayer(player, new GlueHighlightPayload(positions));
        }
    }

    /** A broken block is no longer part of anything. */
    @SubscribeEvent
    public static void onBlockBroken(final BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof final ServerLevel level) {
            GlueSavedData.get(level).unglue(event.getPos());
        }
    }

    /**
     * The off-hand is the intended slot, but a player holding glue in the main hand and placing
     * from the off-hand should get the same behaviour.
     */
    private static @Nullable InteractionHand findGlueHand(final Player player) {
        if (player.getOffhandItem().is(CrashyItems.GLUE.get())) {
            return InteractionHand.OFF_HAND;
        }
        if (player.getMainHandItem().is(CrashyItems.GLUE.get())) {
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    public static void showGlueEffect(final ServerLevel level, final BlockPos pos) {
        CrashyEffects.glue(level, pos);
        level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.5F, 1.6F);
    }
}
