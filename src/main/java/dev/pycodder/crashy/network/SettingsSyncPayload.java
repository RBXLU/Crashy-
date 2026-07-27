package dev.pycodder.crashy.network;

import dev.pycodder.crashy.Crashy;
import dev.pycodder.crashy.settings.CrashySettingsData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: the current rules, plus whether this player is allowed to change them.
 *
 * <p>Permission is decided server-side and shipped along, so the screen can grey itself out instead
 * of letting someone edit settings that will be rejected.
 */
public record SettingsSyncPayload(CrashySettingsData settings, boolean canEdit) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SettingsSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Crashy.id("settings_sync"));

    public static final StreamCodec<ByteBuf, SettingsSyncPayload> STREAM_CODEC = StreamCodec.composite(
            CrashySettingsData.STREAM_CODEC, SettingsSyncPayload::settings,
            ByteBufCodecs.BOOL, SettingsSyncPayload::canEdit,
            SettingsSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
