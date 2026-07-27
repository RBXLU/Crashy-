package dev.pycodder.crashy.network;

import dev.pycodder.crashy.Crashy;
import dev.pycodder.crashy.settings.CrashySettingsData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: "please apply these settings". Always re-checked and clamped on arrival. */
public record SettingsUpdatePayload(CrashySettingsData settings) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SettingsUpdatePayload> TYPE =
            new CustomPacketPayload.Type<>(Crashy.id("settings_update"));

    public static final StreamCodec<ByteBuf, SettingsUpdatePayload> STREAM_CODEC =
            CrashySettingsData.STREAM_CODEC.map(SettingsUpdatePayload::new, SettingsUpdatePayload::settings);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
