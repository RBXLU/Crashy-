package dev.pycodder.crashy.launcher;

import dev.pycodder.crashy.CrashyConfig;
import dev.pycodder.crashy.physics.SableBridge;
import dev.pycodder.crashy.registry.CrashyComponents;
import dev.pycodder.crashy.registry.CrashySounds;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Launcher.
 *
 * <ul>
 *     <li>Right-click a physics object to pick it up.</li>
 *     <li>Hold right-click to charge — power climbs to {@link GrabManager#MAX_POWER}.</li>
 *     <li>Let go to fire. Sneak + right-click puts the object down instead.</li>
 * </ul>
 */
public class LauncherItem extends Item {

    private static final int USE_DURATION = 72_000;

    public LauncherItem(final Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------ picking up

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        final Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }

        final ItemStack stack = context.getItemInHand();
        final InteractionHand hand = context.getHand();

        if (isGrabbing(stack)) {
            return beginChargeOrDrop(level, player, hand) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }

        if (!(player instanceof final ServerPlayer serverPlayer)) {
            // Client-side: the server decides, we only avoid playing the wrong animation.
            return InteractionResult.SUCCESS;
        }

        final ServerSubLevel subLevel = SableBridge.subLevelAt(serverPlayer.serverLevel(), context.getClickedPos());
        if (subLevel == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.crashy.launcher.not_physics")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        GrabManager.grab(serverPlayer, subLevel, hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);

        if (isGrabbing(stack)) {
            return beginChargeOrDrop(level, player, hand)
                    ? InteractionResultHolder.consume(stack)
                    : InteractionResultHolder.success(stack);
        }

        if (player instanceof final ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable("message.crashy.launcher.aim")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResultHolder.fail(stack);
    }

    /** @return true if the charge-up started, false if the object was dropped instead. */
    private static boolean beginChargeOrDrop(final Level level, final Player player, final InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (player instanceof final ServerPlayer serverPlayer) {
                GrabManager.release(serverPlayer);
            }
            return false;
        }
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
        }
        return true;
    }

    // ------------------------------------------------------------------ charging

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(final Level level, final LivingEntity entity, final ItemStack stack, final int remainingUseDuration) {
        if (!(entity instanceof final ServerPlayer player)) {
            return;
        }
        if (!GrabManager.isGrabbing(player)) {
            player.stopUsingItem();
            return;
        }

        final int used = USE_DURATION - remainingUseDuration;
        final int power = GrabManager.powerFor(used);

        player.displayClientMessage(powerBar(power), true);

        final int perPower = Math.max(1, CrashyConfig.TICKS_PER_POWER.get());

        if (used > 0 && used % perPower == 0) {
            if (power < GrabManager.MAX_POWER) {
                // One click per new power level makes the charge audible.
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.5F, 0.6F + power * 0.1F);
            } else if (used == perPower * GrabManager.MAX_POWER) {
                // Exactly once, the moment the charge tops out.
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        CrashySounds.CHARGED.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            }
        }
    }

    @Override
    public void releaseUsing(final ItemStack stack, final Level level, final LivingEntity entity, final int timeLeft) {
        if (!(entity instanceof final ServerPlayer player)) {
            return;
        }

        final int power = GrabManager.powerFor(USE_DURATION - timeLeft);
        GrabManager.launch(player, power);

        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(power, player, player.getUsedItemHand() == InteractionHand.OFF_HAND
                    ? EquipmentSlot.OFFHAND
                    : EquipmentSlot.MAINHAND);
        }
    }

    // ------------------------------------------------------------------ presentation

    @Override
    public void appendHoverText(final ItemStack stack,
                                final TooltipContext context,
                                final List<Component> tooltip,
                                final TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.crashy.launcher.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.crashy.launcher.2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.crashy.launcher.3").withStyle(ChatFormatting.DARK_GRAY));
        if (isGrabbing(stack)) {
            tooltip.add(Component.translatable("tooltip.crashy.launcher.holding").withStyle(ChatFormatting.AQUA));
        }
    }

    private static boolean isGrabbing(final ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(CrashyComponents.GRABBING.get()));
    }

    private static MutableComponent powerBar(final int power) {
        final StringBuilder bar = new StringBuilder();
        for (int i = 1; i <= GrabManager.MAX_POWER; i++) {
            bar.append(i <= power ? '■' : '□');
        }

        final ChatFormatting colour = power >= GrabManager.MAX_POWER
                ? ChatFormatting.RED
                : power >= 7 ? ChatFormatting.GOLD : ChatFormatting.YELLOW;

        return Component.translatable("message.crashy.launcher.power",
                Component.literal(bar.toString()).withStyle(colour),
                Component.literal(String.valueOf(power)).withStyle(colour));
    }
}
