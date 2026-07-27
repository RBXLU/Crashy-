package dev.pycodder.crashy.settings;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** How a crash is allowed to treat the blocks it touches. */
public enum DestructionMode implements StringRepresentable {

    /** Everything inside the blast radius shatters, whatever it is made of. */
    INSTANT("instant"),

    /**
     * The blast has to actually overcome the block. Force falls off with distance and is measured
     * against the block's blast resistance, so a hit that scatters planks only chips cobblestone.
     */
    REALISTIC("realistic"),

    /** Nothing breaks. Objects still fly and bounce, the world just survives them. */
    INDESTRUCTIBLE("indestructible");

    public static final Codec<DestructionMode> CODEC = StringRepresentable.fromEnum(DestructionMode::values);

    private final String name;

    DestructionMode(final String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Component displayName() {
        return Component.translatable("settings.crashy.mode." + this.name);
    }

    public Component description() {
        return Component.translatable("settings.crashy.mode." + this.name + ".desc");
    }
}
