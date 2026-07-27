package dev.pycodder.crashy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.pycodder.crashy.Crashy;
import dev.pycodder.crashy.registry.CrashyItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/** Draws a soft outline around every glued block while the player is holding glue. */
@EventBusSubscriber(modid = Crashy.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class GlueHighlightRenderer {

    private static final float R = 0.36F;
    private static final float G = 0.86F;
    private static final float B = 0.92F;

    private GlueHighlightRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null || !holdsGlue(player)) {
            return;
        }

        final List<BlockPos> positions = GlueHighlightState.current();
        if (positions.isEmpty()) {
            return;
        }

        // Gentle pulse so the outline reads as an overlay rather than part of the world.
        final float time = (player.tickCount + event.getPartialTick().getGameTimeDeltaPartialTick(false)) * 0.12F;
        final float alpha = 0.42F + 0.18F * Mth.sin(time);

        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack poseStack = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (final BlockPos pos : positions) {
            LevelRenderer.renderLineBox(poseStack, lines, new AABB(pos).inflate(0.0035), R, G, B, alpha);
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static boolean holdsGlue(final LocalPlayer player) {
        return player.getMainHandItem().is(CrashyItems.GLUE.get())
                || player.getOffhandItem().is(CrashyItems.GLUE.get());
    }
}
