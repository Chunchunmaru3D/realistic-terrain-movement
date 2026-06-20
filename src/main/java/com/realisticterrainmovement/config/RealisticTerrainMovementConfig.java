package com.realisticterrainmovement.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class RealisticTerrainMovementConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ── Master feature toggles ───────────────────────────────────────────────
    // Each major mechanic can be switched off independently and completely.

    public static final ModConfigSpec.ConfigValue<Boolean> TERRAIN_ENABLED;
    public static final ModConfigSpec.ConfigValue<Boolean> WIND_ENABLED;
    public static final ModConfigSpec.ConfigValue<Boolean> BOATS_ENABLED;

    // ── General ───────────────────────────────────────────────────────────────

    public static final ModConfigSpec.ConfigValue<Boolean> AFFECT_MOBS;
    public static final ModConfigSpec.ConfigValue<Boolean> AFFECT_FLYING;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_ENTITY_TYPES;

    // ── Terrain / Block modifiers ─────────────────────────────────────────────

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TERRAIN_MODIFIERS;
    public static final ModConfigSpec.ConfigValue<Integer> TERRAIN_STICKY_TICKS;
    public static final ModConfigSpec.ConfigValue<Boolean> JUMP_DISTANCE_LIMIT_ENABLED;
    public static final ModConfigSpec.ConfigValue<Double> JUMP_DISTANCE_LIMIT_FLOOR;

    // ── Boots ─────────────────────────────────────────────────────────────────

    public static final ModConfigSpec.ConfigValue<Boolean> BOOTS_REDUCTION_ENABLED;
    public static final ModConfigSpec.ConfigValue<Double> BOOTS_PENALTY_REDUCTION;

    // ── Wind (high altitude) ─────────────────────────────────────────────────

    public static final ModConfigSpec.ConfigValue<Integer> WIND_START_Y;
    public static final ModConfigSpec.ConfigValue<Integer> WIND_MAX_Y;
    public static final ModConfigSpec.ConfigValue<Double> WIND_MAX_PUSH;
    public static final ModConfigSpec.ConfigValue<Double> WIND_SLOW_FACTOR;
    public static final ModConfigSpec.ConfigValue<Integer> WIND_DIRECTION_PERIOD_TICKS;
    public static final ModConfigSpec.ConfigValue<Double> WIND_JUMP_EXEMPTION_HEIGHT;

    // ── Boats ─────────────────────────────────────────────────────────────────

    public static final ModConfigSpec.ConfigValue<Boolean> BREAK_BOATS_IN_OCEAN;

    // ─────────────────────────────────────────────────────────────────────────

    static {
        BUILDER.comment(
            "Altitude & Terrain Penalty — Configuration",
            "Every major mechanic has its own on/off switch under [features].",
            "Disabling a feature here skips its logic entirely (no performance cost when off)."
        );

        BUILDER.push("features");
        TERRAIN_ENABLED = BUILDER
                .comment("Master switch: block-based speed bonuses/penalties + boots reduction (default: true)")
                .define("terrainEnabled", true);
        WIND_ENABLED = BUILDER
                .comment("Master switch: high-altitude wind push/slowdown (default: true)")
                .define("windEnabled", true);
        BOATS_ENABLED = BUILDER
                .comment("Master switch: boats breaking in ocean biomes (default: true)")
                .define("boatsEnabled", true);
        BUILDER.pop();

        BUILDER.push("general");
        AFFECT_MOBS = BUILDER
                .comment("Whether non-player mobs are also affected by terrain/wind speed changes (default: true)")
                .define("affectMobs", true);
        AFFECT_FLYING = BUILDER
                .comment("Whether flying players (creative/spectator) are affected (default: false)")
                .define("affectFlying", false);
        EXCLUDED_ENTITY_TYPES = BUILDER
                .comment(
                    "Entity type IDs always excluded from speed changes (legless/flying creatures).",
                    "Aquatic mobs (fish, squid, dolphin, etc.) are always excluded automatically."
                )
                .defineListAllowEmpty("excludedEntityTypes",
                        List.of(
                                "minecraft:slime",
                                "minecraft:magma_cube",
                                "minecraft:shulker",
                                "minecraft:ghast",
                                "minecraft:phantom",
                                "minecraft:bat",
                                "minecraft:vex",
                                "minecraft:allay",
                                "minecraft:blaze",
                                "minecraft:wither",
                                "minecraft:ender_dragon"
                        ),
                        entry -> entry instanceof String
                );
        BUILDER.pop();

        BUILDER.push("terrain");
        TERRAIN_MODIFIERS = BUILDER
                .comment(
                    "Signed block speed modifiers. Has no effect if [features].terrainEnabled = false.",
                    "Format: \"namespace:block=value\" or \"#namespace:tag=value\".",
                    "Positive = speed bonus (0.20 = +20%). Negative = speed penalty (-0.20 = -20%).",
                    "Both the block stood ON and the block stood IN are checked; values from both are summed.",
                    "Entries using tags/blocks from a mod that isn't installed are simply ignored — safe to leave in.",
                    "",
                    "Vanilla tags/blocks plus popular biome-mod compatibility (Biomes O'Plenty, Regions Unexplored,",
                    "Geophilic, Quark, Nature's Spirit).",
                    "Add more entries for other mods using the same '#modid:tag=value' or 'modid:block=value' syntax."
                )
                .defineListAllowEmpty("blockSpeedModifiers",
                        List.of(
                                // ── Vanilla bonuses: paths, planks, bricks/chiseled stone ──
                                "minecraft:dirt_path=0.20",
                                "#minecraft:planks=0.20",
                                "minecraft:bricks=0.20",
                                "minecraft:stone_bricks=0.20",
                                "minecraft:chiseled_stone_bricks=0.20",
                                "minecraft:mossy_stone_bricks=0.20",
                                "minecraft:cracked_stone_bricks=0.20",
                                "minecraft:chiseled_sandstone=0.20",
                                "minecraft:chiseled_red_sandstone=0.20",
                                "minecraft:chiseled_quartz_block=0.20",
                                "minecraft:chiseled_nether_bricks=0.20",
                                "minecraft:nether_bricks=0.20",
                                "minecraft:red_nether_bricks=0.20",
                                "minecraft:chiseled_deepslate=0.20",
                                "minecraft:deepslate_bricks=0.20",
                                "minecraft:cracked_deepslate_bricks=0.20",
                                "minecraft:polished_blackstone_bricks=0.20",
                                "minecraft:cracked_polished_blackstone_bricks=0.20",
                                "minecraft:chiseled_polished_blackstone=0.20",
                                "minecraft:end_stone_bricks=0.20",
                                "minecraft:chiseled_tuff=0.20",
                                "minecraft:chiseled_tuff_bricks=0.20",
                                "minecraft:tuff_bricks=0.20",
                                "minecraft:chiseled_copper=0.20",

                                // ── Vanilla penalties: dirt, soft/loose terrain ──
                                "#minecraft:dirt=-0.05",
                                "minecraft:sand=-0.20",
                                "minecraft:red_sand=-0.20",
                                "minecraft:gravel=-0.10",
                                "minecraft:soul_sand=-0.50",
                                "minecraft:soul_soil=-0.40",
                                "minecraft:powder_snow=-0.60",
                                "minecraft:mud=-0.35",
                                "minecraft:muddy_mangrove_roots=-0.25",
                                "minecraft:snow=-0.35",
                                "#minecraft:ice=-0.10",

                                // ── Biomes O'Plenty (modid: biomesoplenty) ──
                                "#biomesoplenty:planks=0.20",
                                "biomesoplenty:dried_mud=-0.15",
                                "biomesoplenty:mud=-0.35",
                                "biomesoplenty:soft_mud=-0.40",
                                "biomesoplenty:black_sand=-0.20",
                                "biomesoplenty:brown_mud=-0.35",
                                "biomesoplenty:ash=-0.20",
                                "biomesoplenty:white_sand=-0.20",
                                "biomesoplenty:silt=-0.30",
                                "biomesoplenty:peat=-0.25",
                                "biomesoplenty:origin_sand=-0.20",

                                // ── Regions Unexplored (modid: regions_unexplored) ──
                                "regions_unexplored:silt=-0.30",
                                "regions_unexplored:red_quicksand=-0.40",
                                "regions_unexplored:quicksand=-0.40",
                                "regions_unexplored:white_sand=-0.20",
                                "regions_unexplored:pink_sand=-0.20",
                                "regions_unexplored:ash=-0.20",
                                "regions_unexplored:cracked_mud=-0.20",
                                "regions_unexplored:peat=-0.25",
                                "regions_unexplored:caustic_sand=-0.20",
                                "#regions_unexplored:planks=0.20",

                                // ── Quark (modid: quark) ──
                                "#quark:planks=0.20",
                                "quark:permafrost=-0.10",
                                "quark:framed_path=0.20",

                                // ── Nature's Spirit (modid: natures_spirit) ──
                                // Most new planks already join the vanilla #minecraft:planks tag,
                                // but listed explicitly here too as a safe redundant fallback.
                                "#natures_spirit:planks=0.20",
                                "natures_spirit:wisteria_planks=0.20",
                                "natures_spirit:aspen_planks=0.20",
                                "natures_spirit:maple_planks=0.20",
                                "natures_spirit:redwood_planks=0.20",
                                "natures_spirit:joshua_planks=0.20",
                                "natures_spirit:cedar_planks=0.20",
                                "natures_spirit:mahogany_planks=0.20",
                                "natures_spirit:olive_planks=0.20",
                                "natures_spirit:saxaul_planks=0.20",
                                "natures_spirit:willow_planks=0.20",
                                "natures_spirit:ghaf_planks=0.20",
                                "natures_spirit:larch_planks=0.20",
                                "natures_spirit:alluaudia_planks=0.20",
                                "natures_spirit:kaolin=0.20",
                                "natures_spirit:kaolin_bricks=0.20",
                                "natures_spirit:chalk=0.20",
                                "natures_spirit:chert=0.20",
                                "natures_spirit:travertine=0.20",
                                "natures_spirit:pink_sandstone=0.20"
                        ),
                        entry -> entry instanceof String s && s.contains("=")
                );
        TERRAIN_STICKY_TICKS = BUILDER
                .comment(
                    "How many ticks (20 = 1 second) a terrain speed modifier is held after leaving the ground.",
                    "Prevents jump-spamming across slow terrain (sand, mud, etc.) to bypass the penalty.",
                    "Default: 40 (2 seconds)"
                )
                .defineInRange("terrainStickyTicks", 40, 0, 1200);
        JUMP_DISTANCE_LIMIT_ENABLED = BUILDER
                .comment(
                    "Whether jumping on a slowing (penalty) block dampens horizontal jump distance.",
                    "Vertical jump height is never affected — only the horizontal carry. (default: true)"
                )
                .define("jumpDistanceLimitEnabled", true);
        JUMP_DISTANCE_LIMIT_FLOOR = BUILDER
                .comment("Minimum allowed horizontal jump-speed fraction even at maximum penalty (default: 0.3 = at least 30% carry)")
                .defineInRange("jumpDistanceLimitFloor", 0.3, 0.05, 1.0);
        BUILDER.pop();

        BUILDER.push("boots");
        BOOTS_REDUCTION_ENABLED = BUILDER
                .comment("Whether equipping boots/feet armor reduces terrain penalties at all (default: true)")
                .define("bootsReductionEnabled", true);
        BOOTS_PENALTY_REDUCTION = BUILDER
                .comment("Multiplier applied to total speed PENALTIES when boots/feet armor is equipped (default: 0.5 = penalties halved)")
                .defineInRange("bootsPenaltyReduction", 0.5, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("wind");
        WIND_START_Y = BUILDER
                .comment("Y-level where wind begins (default: 120)")
                .defineInRange("windStartY", 120, -64, 320);
        WIND_MAX_Y = BUILDER
                .comment("Y-level where wind reaches full strength (default: 250)")
                .defineInRange("windMaxY", 250, -64, 320);
        WIND_MAX_PUSH = BUILDER
                .comment("Maximum sideways push per tick in blocks at full wind strength (default: 0.03)")
                .defineInRange("windMaxPush", 0.03, 0.0, 1.0);
        WIND_SLOW_FACTOR = BUILDER
                .comment("How much walking directly against the wind is slowed at full strength (default: 0.2 = up to -20%)")
                .defineInRange("windSlowFactor", 0.2, 0.0, 0.9);
        WIND_DIRECTION_PERIOD_TICKS = BUILDER
                .comment("Ticks for wind direction to fully rotate 360°, simulating gusty wind (default: 6000 = 5 minutes)")
                .defineInRange("windDirectionPeriodTicks", 6000, 200, 1000000);
        WIND_JUMP_EXEMPTION_HEIGHT = BUILDER
                .comment("Wind is ignored while ascending less than this many blocks above last ground position (default: 3.0)")
                .defineInRange("windJumpExemptionHeight", 3.0, 0.0, 10.0);
        BUILDER.pop();

        BUILDER.push("boats");
        BREAK_BOATS_IN_OCEAN = BUILDER
                .comment("Whether boats break instantly when placed in water within ocean biomes (default: true). Also controlled by [features].boatsEnabled.")
                .define("breakBoatsInOcean", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
