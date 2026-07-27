package dev.pycodder.crashy.glue;

import net.minecraft.ChatFormatting;
import dev.pycodder.crashy.physics.CrashyEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Construction glue.
 *
 * <ul>
 *     <li>Held in the off-hand while building: every block you place is glued automatically and
 *         merged with the glued blocks it touches (see {@link GlueEvents}).</li>
 *     <li>Right-click a block: glue that single block.</li>
 *     <li>Sneak + right-click a glued block: dissolve the whole group.</li>
 * </ul>
 */
public class GlueItem extends Item {

    public GlueItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final Player player = context.getPlayer();
        final BlockPos pos = context.getClickedPos();

        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof final ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return InteractionResult.PASS;
        }

        final GlueSavedData glue = GlueSavedData.get(serverLevel);

        if (player.isShiftKeyDown()) {
            final int removed = glue.unglueGroup(pos);
            if (removed == 0) {
                player.displayClientMessage(Component.translatable("message.crashy.glue.not_glued")
                        .withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            CrashyEffects.unglue(serverLevel, pos);
            serverLevel.playSound(null, pos, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 0.7F, 1.4F);
            player.displayClientMessage(Component.translatable("message.crashy.glue.dissolved", removed)
                    .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }

        if (!glue.glue(pos)) {
            player.displayClientMessage(Component.translatable("message.crashy.glue.limit")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        GlueEvents.showGlueEffect(serverLevel, pos);
        player.displayClientMessage(Component.translatable("message.crashy.glue.glued", glue.getGroupSize(pos))
                .withStyle(ChatFormatting.AQUA), true);

        if (!player.getAbilities().instabuild) {
            final ItemStack stack = context.getItemInHand();
            stack.hurtAndBreak(1, player, context.getHand() == InteractionHand.MAIN_HAND
                    ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                    : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(final ItemStack stack,
                                final TooltipContext context,
                                final List<Component> tooltip,
                                final TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.crashy.glue.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.crashy.glue.2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.crashy.glue.3").withStyle(ChatFormatting.DARK_GRAY));
    }

}
