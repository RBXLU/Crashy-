package dev.pycodder.crashy.network;

import dev.pycodder.crashy.Crashy;
import dev.pycodder.crashy.client.ClientSettings;
import dev.pycodder.crashy.client.GlueHighlightState;
import dev.pycodder.crashy.settings.CrashySettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CrashyNetwork {

    private CrashyNetwork() {
    }

    @EventBusSubscriber(modid = Crashy.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {

        private Registration() {
        }

        @SubscribeEvent
        public static void register(final RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1");

            // The client handlers only ever run on a client. GlueHighlightState and ClientSettings
            // are plain data holders with no client-only imports, so naming them here is safe on a
            // dedicated server too.
            registrar.playToClient(
                    GlueHighlightPayload.TYPE,
                    GlueHighlightPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> GlueHighlightState.accept(payload.positions())));

            registrar.playToClient(
                    SettingsSyncPayload.TYPE,
                    SettingsSyncPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(
                            () -> ClientSettings.accept(payload.settings(), payload.canEdit())));

            registrar.playToServer(
                    SettingsUpdatePayload.TYPE,
                    SettingsUpdatePayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof final ServerPlayer player) {
                            applySettings(player, payload);
                        }
                    }));
        }
    }

    /** Server-side handling of a settings change request. */
    private static void applySettings(final ServerPlayer player, final SettingsUpdatePayload payload) {
        if (!canEdit(player)) {
            // Someone edited the packet or lost permission mid-screen: quietly put them straight.
            PacketDistributor.sendToPlayer(player, new SettingsSyncPayload(
                    CrashySettings.of(player.server), false));
            return;
        }

        CrashySettings.get(player.server).set(payload.settings());
        broadcast(player.server);
    }

    /** Only operators, or the host of a single-player world, may change the rules. */
    public static boolean canEdit(final ServerPlayer player) {
        final MinecraftServer server = player.server;
        return player.hasPermissions(2) || server.isSingleplayerOwner(player.getGameProfile());
    }

    public static void sendTo(final ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new SettingsSyncPayload(CrashySettings.of(player.server), canEdit(player)));
    }

    public static void broadcast(final MinecraftServer server) {
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }

    /** Everyone needs the rules the moment they arrive, before they touch anything. */
    @EventBusSubscriber(modid = Crashy.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static final class Join {

        private Join() {
        }

        @SubscribeEvent
        public static void onLogin(final PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof final ServerPlayer player) {
                sendTo(player);
            }
        }
    }
}
