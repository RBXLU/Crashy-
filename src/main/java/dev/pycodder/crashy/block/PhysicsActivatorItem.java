package dev.pycodder.crashy.block;

import dev.pycodder.crashy.physics.PhysicsActivation;
import dev.pycodder.crashy.physics.SableBridge;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The activator in item form. Placing it works as usual, but right-clicking a live physics object
 * with it switches that object's physics back off and settles it into the world.
 */
public class PhysicsActivatorItem extends BlockItem {

    public PhysicsActivatorItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        // Only the server can tell a plot block from a world block, so it decides. A client that
        // guessed wrong and predicted a placement gets corrected by the usual block update.
        if (context.getPlayer() instanceof final ServerPlayer player) {
            final ServerSubLevel subLevel = SableBridge.subLevelAt(player.serverLevel(), context.getClickedPos());
            if (subLevel != null) {
                PhysicsActivation.deactivate(player.serverLevel(), subLevel, player);
                return InteractionResult.CONSUME;
            }
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(final ItemStack stack,
                                final Item.TooltipContext context,
                                final List<Component> tooltip,
                                final TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.crashy.activator.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.crashy.activator.2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.crashy.activator.3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
