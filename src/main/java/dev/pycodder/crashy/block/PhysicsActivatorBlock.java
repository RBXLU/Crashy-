package dev.pycodder.crashy.block;

import com.mojang.serialization.MapCodec;
import dev.pycodder.crashy.physics.PhysicsActivation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Place it on a glued build and switch it on — the build becomes a real rigid body.
 *
 * <p>Activation happens on empty-hand right-click or on a redstone pulse.
 */
public class PhysicsActivatorBlock extends Block {

    public static final MapCodec<PhysicsActivatorBlock> CODEC = simpleCodec(PhysicsActivatorBlock::new);

    /** False while the activator is still armed, true once it has turned its build into an object. */
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public PhysicsActivatorBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TRIGGERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(TRIGGERED, false);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state,
                                               final Level level,
                                               final BlockPos pos,
                                               final Player player,
                                               final BlockHitResult hit) {
        if (!(level instanceof final ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        if (state.getValue(TRIGGERED)) {
            player.displayClientMessage(Component.translatable("message.crashy.activator.already")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        PhysicsActivation.activate(serverLevel, pos, player);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void neighborChanged(final BlockState state,
                                   final Level level,
                                   final BlockPos pos,
                                   final Block neighborBlock,
                                   final BlockPos neighborPos,
                                   final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);

        if (level instanceof final ServerLevel serverLevel
                && !state.getValue(TRIGGERED)
                && level.hasNeighborSignal(pos)) {
            PhysicsActivation.activate(serverLevel, pos, null);
        }
    }

    @Override
    public void appendHoverText(final ItemStack stack,
                                final Item.TooltipContext context,
                                final List<Component> tooltip,
                                final TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.crashy.activator.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.crashy.activator.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
