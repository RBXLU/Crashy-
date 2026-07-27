package dev.pycodder.crashy.structure;

import dev.pycodder.crashy.physics.CrashyEffects;
import dev.pycodder.crashy.registry.CrashyComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.List;

/**
 * Carries a build recorded by a {@link BuildSaverBlock} and stamps it back out.
 *
 * <ul>
 *     <li>Right-click a block face: paste the build there, already glued.</li>
 *     <li>Sneak + right-click: wipe the blueprint.</li>
 * </ul>
 */
public class BlueprintItem extends Item {

    public BlueprintItem(final Properties properties) {
        super(properties);
    }

    public static @Nullable StructureData read(final ItemStack stack) {
        return stack.get(CrashyComponents.STRUCTURE.get());
    }

    public static void store(final ItemStack stack, final StructureData structure) {
        stack.set(CrashyComponents.STRUCTURE.get(), structure);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        if (!(context.getPlayer() instanceof final ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }

        final ItemStack stack = context.getItemInHand();
        final ServerLevel level = player.serverLevel();

        if (player.isShiftKeyDown()) {
            stack.remove(CrashyComponents.STRUCTURE.get());
            level.playSound(null, context.getClickedPos(), SoundEvents.BOOK_PAGE_TURN,
                    SoundSource.PLAYERS, 0.8F, 0.7F);
            player.displayClientMessage(Component.translatable("message.crashy.blueprint.cleared")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        final StructureData structure = read(stack);
        if (structure == null || structure.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.crashy.blueprint.empty")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        // The clicked face is where the saver stood, so the build lands the same way round.
        final BlockPos origin = context.getClickedPos().relative(context.getClickedFace());
        final int placed = structure.paste(level, origin, true);

        if (placed == 0) {
            player.displayClientMessage(Component.translatable("message.crashy.blueprint.blocked")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        final Vec3i size = structure.dimensions();
        CrashyEffects.blueprint(level,
                new Vector3d(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5),
                Math.max(1.5, Math.max(size.getX(), size.getZ()) * 0.5));
        level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.9F);

        player.displayClientMessage(
                Component.translatable("message.crashy.blueprint.pasted", placed, structure.size())
                        .withStyle(ChatFormatting.GREEN),
                true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(final ItemStack stack,
                                final TooltipContext context,
                                final List<Component> tooltip,
                                final TooltipFlag flag) {
        final StructureData structure = read(stack);

        if (structure == null || structure.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.crashy.blueprint.empty").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            final Vec3i size = structure.dimensions();
            tooltip.add(Component.translatable("tooltip.crashy.blueprint.stored", structure.size())
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.crashy.blueprint.size",
                    size.getX(), size.getY(), size.getZ()).withStyle(ChatFormatting.DARK_GRAY));
        }

        tooltip.add(Component.translatable("tooltip.crashy.blueprint.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.crashy.blueprint.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
