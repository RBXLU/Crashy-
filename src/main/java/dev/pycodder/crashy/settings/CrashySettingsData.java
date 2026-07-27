package dev.pycodder.crashy.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pycodder.crashy.CrashyConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * The settings players can change in-game, as opposed to the caps and safety limits that stay in
 * the config file. Values start from the config and are then owned by the world.
 */
public record CrashySettingsData(
        DestructionMode mode,
        boolean destroyWorld,
        boolean settleDebris,
        boolean tntBlasts,
        double radiusScale,
        double scatterScale,
        double toughnessScale,
        double speedPerPower,
        double debrisRestSeconds) {

    public static final double MIN_SCALE = 0.0;
    public static final double MAX_RADIUS_SCALE = 3.0;
    public static final double MAX_SCATTER_SCALE = 3.0;
    public static final double MIN_TOUGHNESS = 0.1;
    public static final double MAX_TOUGHNESS = 5.0;
    public static final double MIN_SPEED_PER_POWER = 1.0;
    public static final double MAX_SPEED_PER_POWER = 40.0;
    public static final double MIN_DEBRIS_REST = 0.0;
    public static final double MAX_DEBRIS_REST = 120.0;

    public static final Codec<CrashySettingsData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DestructionMode.CODEC.optionalFieldOf("mode", DestructionMode.INSTANT).forGetter(CrashySettingsData::mode),
            Codec.BOOL.optionalFieldOf("destroy_world", true).forGetter(CrashySettingsData::destroyWorld),
            Codec.BOOL.optionalFieldOf("settle_debris", true).forGetter(CrashySettingsData::settleDebris),
            Codec.BOOL.optionalFieldOf("tnt_blasts", true).forGetter(CrashySettingsData::tntBlasts),
            Codec.DOUBLE.optionalFieldOf("radius_scale", 1.0).forGetter(CrashySettingsData::radiusScale),
            Codec.DOUBLE.optionalFieldOf("scatter_scale", 1.0).forGetter(CrashySettingsData::scatterScale),
            Codec.DOUBLE.optionalFieldOf("toughness_scale", 1.0).forGetter(CrashySettingsData::toughnessScale),
            Codec.DOUBLE.optionalFieldOf("speed_per_power", 8.0).forGetter(CrashySettingsData::speedPerPower),
            Codec.DOUBLE.optionalFieldOf("debris_rest_seconds", 10.0).forGetter(CrashySettingsData::debrisRestSeconds)
    ).apply(instance, CrashySettingsData::new));

    public static final StreamCodec<ByteBuf, CrashySettingsData> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    /**
     * Stand-in used before the server has told us anything.
     *
     * <p>Not {@link #fromConfig()}: that reads the SERVER config, which a client sitting on the
     * title screen has not loaded.
     */
    public static final CrashySettingsData DEFAULT = new CrashySettingsData(
            DestructionMode.INSTANT, true, true, true, 1.0, 1.0, 1.0, 8.0, 10.0);

    /** Starting point for a fresh world: whatever the server owner put in the config file. */
    public static CrashySettingsData fromConfig() {
        return new CrashySettingsData(
                DestructionMode.INSTANT,
                CrashyConfig.DESTROY_WORLD.get(),
                CrashyConfig.SETTLE_DEBRIS.get(),
                CrashyConfig.TNT_BLASTS.get(),
                1.0,
                1.0,
                1.0,
                CrashyConfig.SPEED_PER_POWER.get(),
                CrashyConfig.DEBRIS_SETTLE_DELAY.get() / 20.0);
    }

    /** Clamps anything that arrived over the network before it is trusted. */
    public CrashySettingsData sanitised() {
        return new CrashySettingsData(
                this.mode == null ? DestructionMode.INSTANT : this.mode,
                this.destroyWorld,
                this.settleDebris,
                this.tntBlasts,
                Mth.clamp(this.radiusScale, MIN_SCALE, MAX_RADIUS_SCALE),
                Mth.clamp(this.scatterScale, MIN_SCALE, MAX_SCATTER_SCALE),
                Mth.clamp(this.toughnessScale, MIN_TOUGHNESS, MAX_TOUGHNESS),
                Mth.clamp(this.speedPerPower, MIN_SPEED_PER_POWER, MAX_SPEED_PER_POWER),
                Mth.clamp(this.debrisRestSeconds, MIN_DEBRIS_REST, MAX_DEBRIS_REST));
    }

    public CrashySettingsData withMode(final DestructionMode value) {
        return new CrashySettingsData(value, this.destroyWorld, this.settleDebris, this.tntBlasts,
                this.radiusScale, this.scatterScale, this.toughnessScale, this.speedPerPower, this.debrisRestSeconds);
    }

    public CrashySettingsData withDestroyWorld(final boolean value) {
        return new CrashySettingsData(this.mode, value, this.settleDebris, this.tntBlasts,
                this.radiusScale, this.scatterScale, this.toughnessScale, this.speedPerPower, this.debrisRestSeconds);
    }

    public CrashySettingsData withSettleDebris(final boolean value) {
        return new CrashySettingsData(this.mode, this.destroyWorld, value, this.tntBlasts,
                this.radiusScale, this.scatterScale, this.toughnessScale, this.speedPerPower, this.debrisRestSeconds);
    }

    public CrashySettingsData withTntBlasts(final boolean value) {
        return new CrashySettingsData(this.mode, this.destroyWorld, this.settleDebris, value,
                this.radiusScale, this.scatterScale, this.toughnessScale, this.speedPerPower, this.debrisRestSeconds);
    }

    public CrashySettingsData withRadiusScale(final double value) {
        return new CrashySettingsData(this.mode, this.destroyWorld, this.settleDebris, this.tntBlasts,
                value, this.scatterScale, this.toughnessScale, this.speedPerPower, this.debrisRestSeconds);
    }

    public CrashySettingsData withScatterScale(final double value) {
        return new CrashySettingsData(this.mode, this.destroyWorld, this.settleDebris, this.tntBlasts,
                this.radiusScale, value, this.toughnessScale, this.speedPerPower, this.debrisRestSeconds);
    }

    public CrashySettingsData withToughnessScale(final double value) {
        return new CrashySettingsData(this.mode, this.destroyWorld, this.settleDebris, this.tntBlasts,
                this.radiusScale, this.scatterScale, value, this.speedPerPower, this.debrisRestSeconds);
    }

    public CrashySettingsData withSpeedPerPower(final double value) {
        return new CrashySettingsData(this.mode, this.destroyWorld, this.settleDebris, this.tntBlasts,
                this.radiusScale, this.scatterScale, this.toughnessScale, value, this.debrisRestSeconds);
    }

    public CrashySettingsData withDebrisRestSeconds(final double value) {
        return new CrashySettingsData(this.mode, this.destroyWorld, this.settleDebris, this.tntBlasts,
                this.radiusScale, this.scatterScale, this.toughnessScale, this.speedPerPower, value);
    }
}
