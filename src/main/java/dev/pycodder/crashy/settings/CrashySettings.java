package dev.pycodder.crashy.settings;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-wide Crashy settings.
 *
 * <p>Deliberately stored on the overworld and read through the server: these are one set of rules
 * for the whole game, not per-dimension, and everyone needs to agree on them.
 */
public class CrashySettings extends SavedData {

    private static final String NAME = "crashy_settings";
    private static final String KEY = "settings";

    private CrashySettingsData data = CrashySettingsData.fromConfig();

    public static CrashySettings get(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CrashySettings::new, CrashySettings::load),
                NAME);
    }

    /** Convenience for the common "just tell me the current rules" case. */
    public static CrashySettingsData of(final MinecraftServer server) {
        return get(server).data();
    }

    private static CrashySettings load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CrashySettings settings = new CrashySettings();
        final Tag encoded = tag.get(KEY);
        if (encoded != null) {
            CrashySettingsData.CODEC.parse(NbtOps.INSTANCE, encoded)
                    .result()
                    .ifPresent(parsed -> settings.data = parsed.sanitised());
        }
        return settings;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        CrashySettingsData.CODEC.encodeStart(NbtOps.INSTANCE, this.data)
                .result()
                .ifPresent(encoded -> tag.put(KEY, encoded));
        return tag;
    }

    public CrashySettingsData data() {
        return this.data;
    }

    public void set(final CrashySettingsData newData) {
        this.data = newData.sanitised();
        this.setDirty();
    }
}
