package com.screamix.adaptiveapiary.genetics;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Наследование: потомок получает по одному аллелю каждого признака от королевы и от трутня. */
public final class BeeBreeding {
    /** Половина генома: один набор аллелей. */
    private record Half(ResourceLocation species, int speed, int lifespan, int fertility) {
        static Half template(BeeSpecies s) {
            return new Half(s.id(), s.speed(), s.lifespan(), s.fertility());
        }
    }

    /**
     * Создаёт геном потомка.
     *
     * @param level мир пасеки для мутаций, зависящих от окружения (может быть null)
     * @param pos   позиция пасеки (может быть null)
     */
    public static BeeGenome offspring(BeeGenome queen, BeeGenome mate, @Nullable Level level,
            @Nullable BlockPos pos, RandomSource random) {
        Half fromQueen = pick(queen, random);
        Half fromMate = pick(mate, random);

        // Мутация проверяется отдельно для каждой половины, поэтому чаще всего получается гибрид:
        // новый вид в одном аллеле и старый в другом. Чистую линию нужно выводить дальше.
        ResourceLocation first = Mutations.roll(fromQueen.species(), fromMate.species(), level, pos, random);
        ResourceLocation second = Mutations.roll(fromQueen.species(), fromMate.species(), level, pos, random);
        if (first != null) {
            fromQueen = Half.template(SpeciesRegistry.get(first));
        }
        if (second != null) {
            fromMate = Half.template(SpeciesRegistry.get(second));
        }

        Half active = fromQueen;
        Half inactive = fromMate;
        if (random.nextBoolean()) {
            active = fromMate;
            inactive = fromQueen;
        }
        return new BeeGenome(
                active.species(), inactive.species(),
                active.speed(), inactive.speed(),
                active.lifespan(), inactive.lifespan(),
                active.fertility(), inactive.fertility(),
                false);
    }

    private static Half pick(BeeGenome genome, RandomSource random) {
        return new Half(
                random.nextBoolean() ? genome.species() : genome.inactiveSpecies(),
                random.nextBoolean() ? genome.speed() : genome.inactiveSpeed(),
                random.nextBoolean() ? genome.lifespan() : genome.inactiveLifespan(),
                random.nextBoolean() ? genome.fertility() : genome.inactiveFertility());
    }

    private BeeBreeding() {}
}
