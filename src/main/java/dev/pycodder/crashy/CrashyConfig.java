package dev.pycodder.crashy;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side tuning knobs. Everything gameplay-relevant lives here. */
public final class CrashyConfig {

    public static final ModConfigSpec SPEC;

    // ----- glue -----
    public static final ModConfigSpec.IntValue MAX_GLUE_GROUP_SIZE;

    // ----- physics activator -----
    public static final ModConfigSpec.IntValue MAX_ASSEMBLE_BLOCKS;
    public static final ModConfigSpec.BooleanValue ACTIVATOR_FALLBACK_CONNECTED;

    // ----- blueprints -----
    public static final ModConfigSpec.IntValue MAX_BLUEPRINT_BLOCKS;

    // ----- launcher -----
    public static final ModConfigSpec.DoubleValue HOLD_DISTANCE;
    public static final ModConfigSpec.DoubleValue MAX_HOLD_DISTANCE;
    public static final ModConfigSpec.IntValue TICKS_PER_POWER;
    public static final ModConfigSpec.DoubleValue SPEED_PER_POWER;

    // ----- destruction -----
    public static final ModConfigSpec.BooleanValue DESTROY_WORLD;
    public static final ModConfigSpec.DoubleValue MIN_IMPACT_SPEED;
    public static final ModConfigSpec.DoubleValue MAX_DESTRUCTION_RADIUS;
    public static final ModConfigSpec.IntValue MAX_WORLD_BLOCKS_DESTROYED;
    public static final ModConfigSpec.IntValue MAX_DEBRIS_PER_IMPACT;
    public static final ModConfigSpec.DoubleValue DEBRIS_SCATTER;

    // ----- TNT -----
    public static final ModConfigSpec.BooleanValue TNT_BLASTS;
    public static final ModConfigSpec.DoubleValue TNT_BLAST_RADIUS;
    public static final ModConfigSpec.DoubleValue TNT_BLAST_STRENGTH;
    public static final ModConfigSpec.DoubleValue TNT_BLAST_FORCE;

    // ----- debris cleanup -----
    public static final ModConfigSpec.BooleanValue SETTLE_DEBRIS;
    public static final ModConfigSpec.IntValue DEBRIS_MAX_LIFETIME;
    public static final ModConfigSpec.IntValue DEBRIS_SETTLE_DELAY;

    static {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("glue");
        MAX_GLUE_GROUP_SIZE = b
                .comment("Maximum amount of blocks that can belong to a single glued group.")
                .defineInRange("maxGlueGroupSize", 8192, 8, 200_000);
        b.pop();

        b.push("activator");
        MAX_ASSEMBLE_BLOCKS = b
                .comment("Maximum amount of blocks the physics activator will turn into one physics object.")
                .defineInRange("maxAssembleBlocks", 8192, 2, 200_000);
        ACTIVATOR_FALLBACK_CONNECTED = b
                .comment("If the activator is not touching any glued block, assemble every physically connected block instead.")
                .define("fallbackToConnectedBlocks", true);
        b.pop();

        b.push("blueprint");
        MAX_BLUEPRINT_BLOCKS = b
                .comment("Maximum amount of blocks a build saver will record onto a blueprint.")
                .defineInRange("maxBlueprintBlocks", 4096, 1, 100_000);
        b.pop();

        b.push("launcher");
        HOLD_DISTANCE = b
                .comment("How far in front of the player's eyes a grabbed object floats, in blocks.")
                .defineInRange("holdDistance", 5.0, 1.5, 24.0);
        MAX_HOLD_DISTANCE = b
                .comment("The grab is dropped once the object is further away than this, in blocks.")
                .defineInRange("maxHoldDistance", 24.0, 4.0, 96.0);
        TICKS_PER_POWER = b
                .comment("Ticks of holding right-click needed for each power level (power caps at 10).")
                .defineInRange("ticksPerPower", 4, 1, 40);
        SPEED_PER_POWER = b
                .comment("Launch speed added per power level, in m/s. Power 10 therefore launches at 10x this.")
                .defineInRange("speedPerPower", 8.0, 0.5, 40.0);
        b.pop();

        b.push("destruction");
        DESTROY_WORLD = b
                .comment("Whether a crashing object is allowed to shatter the world blocks it hits.")
                .define("destroyWorldBlocks", true);
        MIN_IMPACT_SPEED = b
                .comment("Impacts slower than this (m/s) do not cause destruction.")
                .defineInRange("minImpactSpeed", 6.0, 0.5, 100.0);
        MAX_DESTRUCTION_RADIUS = b
                .comment("Upper bound for the crater radius, in blocks.")
                .defineInRange("maxDestructionRadius", 4.0, 0.0, 16.0);
        MAX_WORLD_BLOCKS_DESTROYED = b
                .comment("Hard cap of world blocks turned into debris by a single impact.")
                .defineInRange("maxWorldBlocksDestroyed", 220, 0, 4000);
        MAX_DEBRIS_PER_IMPACT = b
                .comment("Hard cap of blocks the crashing object itself breaks into. If the object is larger, only the impacted region shatters.")
                .defineInRange("maxDebrisPerImpact", 320, 1, 4000);
        DEBRIS_SCATTER = b
                .comment("How violently debris is thrown away from the impact point, in m/s.",
                        "Set this too low and shards barely move, come to rest instantly and get written",
                        "straight back into the world — which looks like the destruction undoing itself.")
                .defineInRange("debrisScatter", 11.0, 0.0, 60.0);
        b.pop();

        b.push("tnt");
        TNT_BLASTS = b
                .comment("TNT caught in a crash goes off. The block is removed by hand instead of being primed,",
                        "so no vanilla explosion deletes the surroundings: everything nearby survives and flies.")
                .define("explodeTnt", true);
        TNT_BLAST_RADIUS = b
                .comment("Radius of the blast each TNT adds, in blocks.")
                .defineInRange("blastRadius", 5.0, 1.0, 16.0);
        TNT_BLAST_STRENGTH = b
                .comment("How hard a TNT blast throws blocks, in m/s.")
                .defineInRange("blastStrength", 26.0, 1.0, 120.0);
        TNT_BLAST_FORCE = b
                .comment("How much a TNT blast can break through in Realistic mode, measured against",
                        "block blast resistance (planks 3, cobblestone 6, obsidian 1200).")
                .defineInRange("blastForce", 40.0, 0.0, 2000.0);
        b.pop();

        b.push("debris");
        SETTLE_DEBRIS = b
                .comment("Turn resting debris back into ordinary world blocks. Strongly recommended: without it every",
                        "single shard stays a live rigid body forever and the server will grind to a halt.")
                .define("settleDebris", true);
        DEBRIS_MAX_LIFETIME = b
                .comment("Debris is force-settled after this many ticks no matter what.")
                .defineInRange("maxLifetimeTicks", 600, 20, 24000);
        DEBRIS_SETTLE_DELAY = b
                .comment("Ticks a shard has to stay nearly motionless before it settles.")
                .defineInRange("settleDelayTicks", 40, 1, 1200);
        b.pop();

        SPEC = b.build();
    }

    private CrashyConfig() {
    }
}
