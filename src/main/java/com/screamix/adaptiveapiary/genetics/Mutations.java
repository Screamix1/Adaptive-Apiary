package com.screamix.adaptiveapiary.genetics;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.screamix.adaptiveapiary.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Таблица мутаций: какие два вида могут дать новый вид и с каким шансом. */
public final class Mutations {
    /** Мутация: вид из списка first и вид из списка second (в любом порядке) с шансом chance дают result. */
    public record Mutation(List<ResourceLocation> first, List<ResourceLocation> second,
            ResourceLocation result, float chance) {

        boolean matches(ResourceLocation a, ResourceLocation b) {
            return (first.contains(a) && second.contains(b)) || (first.contains(b) && second.contains(a));
        }
    }

    /** Рудная мутация для показа в JEI: результат и блоки, которые нужно поставить под пасеку. */
    public record OreMutation(String material, ResourceLocation result, float chance, List<Item> blocks) {}

    private static final List<Mutation> LIST = new ArrayList<>();

    static {
        List<ResourceLocation> wild = SpeciesRegistry.SURFACE_WILD;

        // Обычная = два разных диких вида (одинаковые виды не мутируют, это проверяется в roll)
        add(wild, wild, SpeciesRegistry.COMMON, 0.15F);
        add(List.of(SpeciesRegistry.COMMON), wild, SpeciesRegistry.CULTIVATED, 0.12F);

        add(SpeciesRegistry.COMMON, SpeciesRegistry.CULTIVATED, SpeciesRegistry.NOBLE, 0.10F);
        add(SpeciesRegistry.NOBLE, SpeciesRegistry.CULTIVATED, SpeciesRegistry.MAJESTIC, 0.08F);
        add(SpeciesRegistry.NOBLE, SpeciesRegistry.MAJESTIC, SpeciesRegistry.IMPERIAL, 0.08F);

        add(SpeciesRegistry.COMMON, SpeciesRegistry.CULTIVATED, SpeciesRegistry.DILIGENT, 0.10F);
        add(SpeciesRegistry.DILIGENT, SpeciesRegistry.CULTIVATED, SpeciesRegistry.UNWEARY, 0.08F);
        add(SpeciesRegistry.DILIGENT, SpeciesRegistry.UNWEARY, SpeciesRegistry.INDUSTRIOUS, 0.08F);

        add(SpeciesRegistry.ROCKY, SpeciesRegistry.DILIGENT, SpeciesRegistry.MINERAL, 0.12F);
    }

    private static void add(ResourceLocation a, ResourceLocation b, ResourceLocation result, float chance) {
        add(List.of(a), List.of(b), result, chance);
    }

    private static void add(List<ResourceLocation> a, List<ResourceLocation> b, ResourceLocation result, float chance) {
        LIST.add(new Mutation(List.copyOf(a), List.copyOf(b), result, chance));
    }

    public static List<Mutation> all() {
        return List.copyOf(LIST);
    }

    /** Рудные мутации для всех найденных материалов. */
    public static List<OreMutation> oreMutations() {
        float chance;
        try {
            chance = Config.ORE_MUTATION_CHANCE.get().floatValue();
        } catch (IllegalStateException e) {
            chance = 0.10F;
        }
        List<OreMutation> list = new ArrayList<>();
        for (Materials.MaterialInfo info : Materials.all().values()) {
            list.add(new OreMutation(info.name(), SpeciesRegistry.oreId(info.name()), chance, info.ores()));
        }
        return list;
    }

    /**
     * Пробует мутацию для пары видов.
     *
     * @param level мир пасеки, null если проверять окружение не нужно
     * @param pos   позиция пасеки
     * @return новый вид или null, если мутации не случилось
     */
    @Nullable
    public static ResourceLocation roll(ResourceLocation a, ResourceLocation b, @Nullable Level level,
            @Nullable BlockPos pos, RandomSource random) {
        if (a.equals(b)) {
            return null;
        }
        for (Mutation mutation : LIST) {
            if (mutation.matches(a, b) && random.nextFloat() < mutation.chance()) {
                return mutation.result();
            }
        }
        // Адаптивная мутация: Рудокопная + Культурная над блоком руды дают пчелу этой руды.
        if (level != null && pos != null && isOreParents(a, b)) {
            String material = Materials.ofBlock(level.getBlockState(pos.below()));
            if (material != null && random.nextDouble() < Config.ORE_MUTATION_CHANCE.get()) {
                return SpeciesRegistry.oreId(material);
            }
        }
        return null;
    }

    public static boolean isOreParents(ResourceLocation a, ResourceLocation b) {
        return (a.equals(SpeciesRegistry.MINERAL) && b.equals(SpeciesRegistry.CULTIVATED))
                || (b.equals(SpeciesRegistry.MINERAL) && a.equals(SpeciesRegistry.CULTIVATED));
    }

    private Mutations() {}
}
