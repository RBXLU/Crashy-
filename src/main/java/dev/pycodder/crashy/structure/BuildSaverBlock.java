package dev.pycodder.crashy.structure;

import com.mojang.serialization.MapCodec;
import dev.pycodder.crashy.CrashyConfig;
import dev.pycodder.crashy.physics.CrashyEffects;
import dev.pycodder.crashy.registry.CrashyItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.List;
import java.util.Set;

/**
 * Records the build it is attached to so it can be stamped out again elsewhere.
 *
 * <ul>
 *     <li>Empty hand: scan and save the build.</li>
 *     <li>Blueprint in hand: copy the saved build onto the blueprint.</li>
 *     <li>Sneak + empty hand: wipe what is stored.</li>
 * </ul>
 */
public class BuildSaverBlock extends BaseEntityBlock {

    public static final MapCodec<BuildSaverBlock> CODEC = simpleCodec(BuildSaverBlock::new);

    /** Lit while a build is stored, so a loaded saver is obvious at a glance. */
    public static final BooleanProperty LOADED = BlockStateProperties.LIT;

    public BuildSaverBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LOADED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOADED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new BuildSaverBlockEntity(pos, state);
    }

    /** BaseEntityBlock hides its block by default; this one is an ordinary looking machine. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult useWithoutItem(final BlockState state,
                                               final Level level,
                                               final BlockPos pos,
                                               final Player player,
                                               final BlockHitResult hit) {
        if (!(level instanceof final ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof final BuildSaverBlockEntity saver)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            saver.setStructure(null);
            level.setBlock(pos, state.setValue(LOADED, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.7F, 0.8F);
            player.displayClientMessage(Component.translatable("message.crashy.saver.cleared")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        scan(serverLevel, pos, state, saver, player);
        return InteractionResult.CONSUME;
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack,
                                              final BlockState state,
                                              final Level level,
                                              final BlockPos pos,
                                              final Player player,
                                              final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (!stack.is(CrashyItems.BLUEPRINT.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level instanceof final ServerLevel serverLevel)) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof final BuildSaverBlockEntity saver)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Handing over a blueprint with nothing stored yet should just work, so scan first.
        StructureData structure = saver.getStructure();
        if (structure == null || structure.isEmpty()) {
            structure = scan(serverLevel, pos, state, saver, player);
            if (structure == null) {
                return ItemInteractionResult.CONSUME;
            }
        }

        BlueprintItem.store(stack, structure);
        serverLevel.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.9F, 1.1F);
        player.displayClientMessage(
                Component.translatable("message.crashy.saver.copied", structure.size())
                        .withStyle(ChatFormatting.AQUA),
                true);
        return ItemInteractionResult.CONSUME;
    }

    private static @Nullable StructureData scan(final ServerLevel level,
                                                final BlockPos pos,
                                                final BlockState state,
                                                final BuildSaverBlockEntity saver,
                                                final Player player) {
        final int max = CrashyConfig.MAX_BLUEPRINT_BLOCKS.get();
        final StructureScanner.Result result = StructureScanner.collect(level, pos, max, true);

        if (result.tooMany()) {
            player.displayClientMessage(Component.translatable("message.crashy.saver.too_many", max)
                    .withStyle(ChatFormatting.RED), true);
            return null;
        }

        // The saver is scaffolding, not part of the build.
        final Set<BlockPos> blocks = result.blocks();
        blocks.removeIf(candidate -> level.getBlockState(candidate).getBlock() instanceof BuildSaverBlock);

        if (blocks.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.crashy.saver.nothing")
                    .withStyle(ChatFormatting.RED), true);
            return null;
        }

        final StructureData structure = StructureData.capture(level, blocks, pos);
        saver.setStructure(structure);
        level.setBlock(pos, state.setValue(LOADED, true), Block.UPDATE_ALL);

        CrashyEffects.blueprint(level,
                new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), 1.5);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.2F);
        player.displayClientMessage(
                Component.translatable("message.crashy.saver.saved", structure.size())
                        .withStyle(ChatFormatting.GREEN),
                true);
        return structure;
    }

    @Override
    public void appendHoverText(final ItemStack stack,
                                final Item.TooltipContext context,
                                final List<Component> tooltip,
                                final TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.crashy.saver.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.crashy.saver.2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.crashy.saver.3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
