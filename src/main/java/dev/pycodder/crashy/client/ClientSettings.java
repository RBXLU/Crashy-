package dev.pycodder.crashy.client;

import dev.pycodder.crashy.settings.CrashySettingsData;

/**
 * The rules the server last told this client about.
 *
 * <p>Like {@link GlueHighlightState}, deliberately free of client-only imports so the network
 * registration that names it is safe to load on a dedicated server.
 */
public final class ClientSettings {

    private static volatile CrashySettingsData settings = CrashySettingsData.DEFAULT;
    private static volatile boolean canEdit = false;

    private ClientSettings() {
    }

    public static void accept(final CrashySettingsData newSettings, final boolean newCanEdit) {
        settings = newSettings;
        canEdit = newCanEdit;
    }

    public static CrashySettingsData get() {
        return settings;
    }

    public static boolean canEdit() {
        return canEdit;
    }
}
