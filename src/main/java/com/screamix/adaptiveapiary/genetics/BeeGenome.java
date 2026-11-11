package com.screamix.adaptiveapiary.genetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Геном пчелы. У каждого признака два аллеля: активный определяет поведение,
 * неактивный просто передаётся потомству.
 * Признаки хранятся индексами, значения см. в {@link Traits}.
 */
public record BeeGenome(
        ResourceLocation species, ResourceLocation inactiveSpecies,
        int speed, int inactiveSpeed,
        int lifespan, int inactiveLifespan,
        int fertility, int inactiveFertility,
        boolean analyzed) {

    public static final Codec<BeeGenome> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("species").forGetter(BeeGenome::species),
            ResourceLocation.CODEC.fieldOf("inactive_species").forGetter(BeeGenome::inactiveSpecies),
            Codec.INT.fieldOf("speed").forGetter(BeeGenome::speed),
            Codec.INT.fieldOf("inactive_speed").forGetter(BeeGenome::inactiveSpeed),
            Codec.INT.fieldOf("lifespan").forGetter(BeeGenome::lifespan),
            Codec.INT.fieldOf("inactive_lifespan").forGetter(BeeGenome::inactiveLifespan),
            Codec.INT.fieldOf("fertility").forGetter(BeeGenome::fertility),
            Codec.INT.fieldOf("inactive_fertility").forGetter(BeeGenome::inactiveFertility),
            Codec.BOOL.optionalFieldOf("analyzed", false).forGetter(BeeGenome::analyzed)
    ).apply(i, BeeGenome::new));

    /** Чистокровная особь вида: оба аллеля каждого признака взяты из шаблона вида. */
    public static BeeGenome pure(BeeSpecies s) {
        return new BeeGenome(s.id(), s.id(), s.speed(), s.speed(), s.lifespan(), s.lifespan(),
                s.fertility(), s.fertility(), false);
    }

    public boolean isPureBred() {
        return species.equals(inactiveSpecies);
    }

    public BeeGenome withAnalyzed(boolean value) {
        return new BeeGenome(species, inactiveSpecies, speed, inactiveSpeed, lifespan, inactiveLifespan,
                fertility, inactiveFertility, value);
    }

    public BeeSpecies activeSpecies() {
        return SpeciesRegistry.get(species);
    }

    public BeeSpecies secondarySpecies() {
        return SpeciesRegistry.get(inactiveSpecies);
    }
}
