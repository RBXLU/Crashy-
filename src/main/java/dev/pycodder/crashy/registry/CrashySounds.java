package dev.pycodder.crashy.registry;

import dev.pycodder.crashy.Crashy;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrashySounds {

    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(Registries.SOUND_EVENT, Crashy.MOD_ID);

    /** The crash itself. Three variants so repeated hits do not sound looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT = register("impact");

    public static final DeferredHolder<SoundEvent, SoundEvent> LAUNCH = register("launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACTIVATE = register("activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGED = register("charged");

    private static DeferredHolder<SoundEvent, SoundEvent> register(final String name) {
        // Variable range: a big crash should carry further than a small one.
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(Crashy.id(name)));
    }

    private CrashySounds() {
    }
}
