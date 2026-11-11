package com.screamix.adaptiveapiary;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue CYCLE_TICKS = BUILDER
            .comment("Length of one bee work cycle in ticks (20 ticks = 1 second). Forestry used 550.")
            .defineInRange("cycleTicks", 300, 20, 24000);

    public static final ModConfigSpec.IntValue BREEDING_TICKS = BUILDER
            .comment("How long a princess and a drone need to become a queen, in ticks.")
            .defineInRange("breedingTicks", 100, 1, 24000);

    public static final ModConfigSpec.IntValue CENTRIFUGE_TICKS = BUILDER
            .comment("How long the centrifuge needs for one comb, in ticks.")
            .defineInRange("centrifugeTicks", 100, 1, 24000);

    public static final ModConfigSpec.BooleanValue ENVIRONMENT_CHECKS = BUILDER
            .comment("If true, bees need flowers, daylight, open sky and dry weather (cave bees ignore sky, night and rain).")
            .define("environmentChecks", true);

    public static final ModConfigSpec.DoubleValue ORE_MUTATION_CHANCE = BUILDER
            .comment("Chance (0..1) that a Mineral x Cultivated pairing mutates into an ore bee",
                    "when a matching ore or storage block is placed under the apiary.")
            .defineInRange("oreMutationChance", 0.10D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue RESOURCE_YIELD = BUILDER
            .comment("Chance (0..1) that one resource comb gives one unit of its material in the centrifuge.")
            .defineInRange("resourceYield", 0.25D, 0.0D, 1.0D);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MATERIAL_BLACKLIST = BUILDER
            .comment("Materials that must NOT get an ore bee. Use the name after 'c:ores/', for example \"diamond\" or \"netherite_scrap\".")
            .defineListAllowEmpty("materialBlacklist", List.of(), () -> "", o -> o instanceof String);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_ORES = BUILDER
            .comment("Manual ore bindings for mods whose ores are not found automatically.",
                    "Format: \"modid:item=material\", for example \"somemod:weird_rock=silver\".",
                    "Use /adaptiveapiary materials in game to see what was found.")
            .defineListAllowEmpty("extraOres", List.of(), () -> "modid:silver_ore=silver", o -> o instanceof String s && s.contains("="));

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
