package dev.pycodder.crashy.network;

import dev.pycodder.crashy.Crashy;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** The glued blocks near a player who is holding glue, so the client can outline them. */
public record GlueHighlightPayload(List<BlockPos> positions) implements CustomPacketPayload {

    public static final int MAX_POSITIONS = 2048;

    public static final CustomPacketPayload.Type<GlueHighlightPayload> TYPE =
            new CustomPacketPayload.Type<>(Crashy.id("glue_highlight"));

    public static final StreamCodec<ByteBuf, GlueHighlightPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC
                    .apply(ByteBufCodecs.list(MAX_POSITIONS))
                    .map(GlueHighlightPayload::new, GlueHighlightPayload::positions);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
