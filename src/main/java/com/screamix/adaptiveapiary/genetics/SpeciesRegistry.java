package com.screamix.adaptiveapiary.genetics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.genetics.BeeSpecies.Product;
import com.screamix.adaptiveapiary.item.CombItem;
import com.screamix.adaptiveapiary.item.ResourceCombItem;
import com.screamix.adaptiveapiary.registry.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Все виды пчёл. Встроенные виды заданы здесь, рудные (id вида "adaptiveapiary:ore/<материал>")
 * собираются по требованию. Клиент и сервер получают один и тот же результат из одного id,
 * поэтому синхронизировать список видов по сети не нужно.
 */
public final class SpeciesRegistry {
    public static final String ORE_PREFIX = "ore/";

    public static final ResourceLocation FOREST = AdaptiveApiary.id("forest");
    public static final ResourceLocation MEADOWS = AdaptiveApiary.id("meadows");
    public static final ResourceLocation MODEST = AdaptiveApiary.id("modest");
    public static final ResourceLocation TROPICAL = AdaptiveApiary.id("tropical");
    public static final ResourceLocation WINTRY = AdaptiveApiary.id("wintry");
    public static final ResourceLocation MARSHY = AdaptiveApiary.id("marshy");
    public static final ResourceLocation ROCKY = AdaptiveApiary.id("rocky");
    public static final ResourceLocation COMMON = AdaptiveApiary.id("common");
    public static final ResourceLocation CULTIVATED = AdaptiveApiary.id("cultivated");
    public static final ResourceLocation NOBLE = AdaptiveApiary.id("noble");
    public static final ResourceLocation MAJESTIC = AdaptiveApiary.id("majestic");
    public static final ResourceLocation IMPERIAL = AdaptiveApiary.id("imperial");
    public static final ResourceLocation DILIGENT = AdaptiveApiary.id("diligent");
    public static final ResourceLocation UNWEARY = AdaptiveApiary.id("unweary");
    public static final ResourceLocation INDUSTRIOUS = AdaptiveApiary.id("industrious");
    public static final ResourceLocation MINERAL = AdaptiveApiary.id("mineral");

    /** Виды, которые живут в диких ульях на поверхности. */
    public static final List<ResourceLocation> SURFACE_WILD = List.of(FOREST, MEADOWS, MODEST, TROPICAL, WINTRY, MARSHY);

    private static final Map<ResourceLocation, BeeSpecies> BUILT_IN = new LinkedHashMap<>();
    private static final Map<String, BeeSpecies> ORE_CACHE = new ConcurrentHashMap<>();

    static {
        // Дикие виды
        add(FOREST, 0x19D0EC, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_SLOWER, Traits.LIFE_SHORTER, 3,
                comb(ModItems.HONEY_COMB, 0.30F));
        add(MEADOWS, 0xEF131E, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_SLOWER, Traits.LIFE_SHORTER, 2,
                comb(ModItems.HONEY_COMB, 0.30F));
        add(MODEST, 0xC5BE86, 0xFFDC16, FlowerType.DESERT, false, Traits.SPEED_SLOWER, Traits.LIFE_SHORT, 2,
                comb(ModItems.PARCHED_COMB, 0.20F));
        add(TROPICAL, 0x378020, 0xFFDC16, FlowerType.JUNGLE, false, Traits.SPEED_SLOWER, Traits.LIFE_SHORT, 2,
                comb(ModItems.SILKY_COMB, 0.20F));
        add(WINTRY, 0xA0FFFC, 0xDAF5F3, FlowerType.FLOWERS, false, Traits.SPEED_SLOWER, Traits.LIFE_SHORT, 4,
                comb(ModItems.FROZEN_COMB, 0.30F));
        add(MARSHY, 0x546626, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_SLOWER, Traits.LIFE_SHORT, 2,
                comb(ModItems.MOSSY_COMB, 0.30F));
        add(ROCKY, 0x6E6E6E, 0xB4B4B4, FlowerType.ROCK, true, Traits.SPEED_SLOWER, Traits.LIFE_SHORT, 2,
                comb(ModItems.ROCKY_COMB, 0.30F));

        // Обычная ветка
        add(COMMON, 0xB2B2B2, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_SLOWER, Traits.LIFE_SHORTER, 2,
                comb(ModItems.HONEY_COMB, 0.35F));
        add(CULTIVATED, 0x5734EC, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_FAST, Traits.LIFE_SHORTEST, 2,
                comb(ModItems.HONEY_COMB, 0.40F));

        // Благородная ветка
        add(NOBLE, 0xEC9A19, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_SLOWER, Traits.LIFE_SHORT, 2,
                comb(ModItems.DRIPPING_COMB, 0.20F));
        add(MAJESTIC, 0x7F0000, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_NORMAL, Traits.LIFE_SHORTENED, 4,
                comb(ModItems.DRIPPING_COMB, 0.30F));
        add(IMPERIAL, 0xA3E02F, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_SLOWER, Traits.LIFE_NORMAL, 2,
                comb(ModItems.DRIPPING_COMB, 0.20F), item(ModItems.HONEYDEW, 0.15F));

        // Трудолюбивая ветка
        add(DILIGENT, 0xC219EC, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_SLOWER, Traits.LIFE_SHORT, 2,
                comb(ModItems.STRINGY_COMB, 0.20F));
        add(UNWEARY, 0x19EC5A, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_NORMAL, Traits.LIFE_SHORTENED, 2,
                comb(ModItems.STRINGY_COMB, 0.30F));
        add(INDUSTRIOUS, 0xFFFFFF, 0xFFDC16, FlowerType.FLOWERS, false, Traits.SPEED_SLOWER, Traits.LIFE_NORMAL, 2,
                comb(ModItems.STRINGY_COMB, 0.20F), item(ModItems.PROPOLIS, 0.15F));

        // Предок всех рудных пчёл
        add(MINERAL, 0xD9A066, 0xA8A8A8, FlowerType.ROCK, true, Traits.SPEED_SLOWER, Traits.LIFE_SHORTENED, 2,
                comb(ModItems.ROCKY_COMB, 0.30F));
    }

    private static Product comb(DeferredItem<CombItem> item, float chance) {
        return new Product(() -> new ItemStack(item.get()), chance);
    }

    private static Product item(DeferredItem<?> item, float chance) {
        return new Product(() -> new ItemStack(item.get()), chance);
    }

    private static void add(ResourceLocation id, int primary, int secondary, FlowerType flowers, boolean cave,
            int speed, int lifespan, int fertility, Product... products) {
        BUILT_IN.put(id, new BeeSpecies(id, primary, secondary, flowers, cave, speed, lifespan, fertility,
                List.of(products), null));
    }

    public static ResourceLocation oreId(String material) {
        return AdaptiveApiary.id(ORE_PREFIX + material);
    }

    /** Возвращает вид по id. Неизвестный id даёт лесную пчелу, чтобы старые предметы не ломали игру. */
    public static BeeSpecies get(ResourceLocation id) {
        BeeSpecies builtIn = BUILT_IN.get(id);
        if (builtIn != null) {
            return builtIn;
        }
        if (AdaptiveApiary.MODID.equals(id.getNamespace()) && id.getPath().startsWith(ORE_PREFIX)) {
            return ore(id.getPath().substring(ORE_PREFIX.length()));
        }
        return BUILT_IN.get(FOREST);
    }

    /** Рудный вид для материала. Создаётся один раз и кэшируется. */
    public static BeeSpecies ore(String material) {
        return ORE_CACHE.computeIfAbsent(material, m -> new BeeSpecies(
                oreId(m), -1, -1, FlowerType.ROCK, true,
                Traits.SPEED_SLOWER, Traits.LIFE_NORMAL, 2,
                List.of(new Product(() -> ResourceCombItem.create(m), 0.20F),
                        new Product(() -> new ItemStack(ModItems.ROCKY_COMB.get()), 0.10F)),
                m));
    }

    public static Collection<BeeSpecies> builtIn() {
        return BUILT_IN.values();
    }

    /** Встроенные виды плюс рудные виды для всех материалов, найденных в текущем наборе модов. */
    public static List<BeeSpecies> allKnown() {
        List<BeeSpecies> list = new ArrayList<>(BUILT_IN.values());
        for (String material : Materials.all().keySet()) {
            list.add(ore(material));
        }
        return list;
    }

    private SpeciesRegistry() {}
}
