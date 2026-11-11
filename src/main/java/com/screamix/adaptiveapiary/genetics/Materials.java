package com.screamix.adaptiveapiary.genetics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.Config;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Адаптивная часть мода: находит руды всех установленных модов и подбирает им продукт.
 *
 * <p>Руда находится любым из способов:
 * <ul>
 *   <li>тег предмета или блока {@code c:ores/<материал>} или старый {@code forge:ores/<материал>};</li>
 *   <li>имя блока: {@code silver_ore}, {@code deepslate_silver_ore}, {@code nether_silver_ore}, {@code ore_silver},
 *       {@code deepslate_ore_silver};</li>
 *   <li>ручная привязка в конфиге ({@code extraOres}).</li>
 * </ul>
 * Продукт ищется по тегам {@code raw_materials/gems/dusts/ingots}, затем по рецепту переплавки руды,
 * затем по имени предмета ({@code raw_silver}, {@code silver_ingot} и т. п.). Если не нашлось ничего,
 * продуктом становится сама руда, так что пчела появляется для любой руды.
 */
public final class Materials {
    private static final List<String> TAG_NAMESPACES = List.of("c", "forge");
    private static final String ORES = "ores/";
    private static final String STORAGE = "storage_blocks/";
    private static final String STORAGE_RAW = "storage_blocks/raw_";
    private static final List<String> PRODUCT_TAGS = List.of("raw_materials/", "gems/", "dusts/", "ingots/");
    /** Приставки породы, которые отрезаются от имени руды: deepslate_silver_ore -> silver. */
    private static final List<String> STONE_PREFIXES = List.of(
            "deepslate", "end_stone", "endstone", "end", "netherrack", "nether", "stone", "granite", "diorite",
            "andesite", "tuff", "calcite", "basalt", "blackstone", "red_sandstone", "sandstone", "gravel", "sand",
            "dense", "poor", "rich", "overworld", "deep", "sculk", "moon", "mars", "venus", "mercury", "glacio");
    private static final Map<String, String> ALIASES = Map.of(
            "aluminium", "aluminum",
            "sulphur", "sulfur",
            "netherite", "netherite_scrap",
            "saltpetre", "saltpeter");

    /** Найденный материал: продукт, предметы руды и откуда мод о нём узнал. */
    public record MaterialInfo(String name, Item product, List<Item> ores, Set<String> sources, String productSource) {
        /** Самая "простая" руда материала (без deepslate_ и т. п.) - для названий и показа в JEI. */
        public Item mainOre() {
            return ores.isEmpty() ? product : ores.get(0);
        }
    }

    @Nullable
    private static volatile Map<String, MaterialInfo> cache;
    @Nullable
    private static volatile Supplier<RecipeManager> clientRecipes;
    @Nullable
    private static volatile Supplier<HolderLookup.Provider> clientRegistries;

    /** Клиент сообщает, откуда брать рецепты и реестры, когда сервера в этом процессе нет. */
    public static void setClientSources(Supplier<RecipeManager> recipes, Supplier<HolderLookup.Provider> registries) {
        clientRecipes = recipes;
        clientRegistries = registries;
    }

    /** Все материалы текущего набора модов, по алфавиту. Пусто, пока теги не загружены. */
    public static Map<String, MaterialInfo> all() {
        Map<String, MaterialInfo> result = cache;
        if (result == null) {
            result = scan();
            // пустой результат не кэшируем: теги могли ещё не загрузиться
            if (!result.isEmpty()) {
                cache = result;
            }
        }
        return result;
    }

    public static void invalidate() {
        cache = null;
    }

    public static boolean exists(String material) {
        return all().containsKey(material);
    }

    /** Один предмет материала, который выдаёт центрифуга, или пустой стек, если материал неизвестен. */
    public static ItemStack product(String material) {
        MaterialInfo info = all().get(material);
        return info == null ? ItemStack.EMPTY : new ItemStack(info.product());
    }

    /** Название материала: перевод, если он есть, иначе название самой руды из её мода. */
    public static Component displayName(String material) {
        String key = "material.adaptiveapiary." + material.replace('/', '.');
        if (Language.getInstance().has(key)) {
            return Component.translatable(key);
        }
        MaterialInfo info = all().get(material);
        if (info != null && !info.ores().isEmpty()) {
            return Component.translatable(info.mainOre().getDescriptionId());
        }
        return Component.literal(prettify(material));
    }

    /**
     * Материал блока под пасекой: подходит руда или блок хранения материала
     * (теги {@code ores/x}, {@code storage_blocks/x}, {@code storage_blocks/raw_x} или имя вроде {@code silver_block}).
     */
    @Nullable
    public static String ofBlock(BlockState state) {
        Map<String, MaterialInfo> known = all();
        if (known.isEmpty()) {
            return null;
        }
        for (TagKey<Block> tag : (Iterable<TagKey<Block>>) state.getTags()::iterator) {
            String m = materialOfTag(tag.location(), true);
            if (m != null && known.containsKey(m)) {
                return m;
            }
        }
        Item item = state.getBlock().asItem();
        if (item != Items.AIR) {
            for (TagKey<Item> tag : (Iterable<TagKey<Item>>) new ItemStack(item).getTags()::iterator) {
                String m = materialOfTag(tag.location(), true);
                if (m != null && known.containsKey(m)) {
                    return m;
                }
            }
            for (MaterialInfo info : known.values()) {
                if (info.ores().contains(item)) {
                    return info.name();
                }
            }
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        String m = materialOfStorageId(path);
        return m != null && known.containsKey(m) ? m : null;
    }

    // ---------------------------------------------------------------- разбор имён

    /** Материал из тега ores/x (и storage_blocks/..., если разрешено). */
    @Nullable
    static String materialOfTag(ResourceLocation tag, boolean allowStorage) {
        if (!TAG_NAMESPACES.contains(tag.getNamespace())) {
            return null;
        }
        String path = tag.getPath();
        String raw = null;
        if (path.startsWith(ORES)) {
            raw = path.substring(ORES.length());
        } else if (allowStorage && path.startsWith(STORAGE_RAW)) {
            raw = path.substring(STORAGE_RAW.length());
        } else if (allowStorage && path.startsWith(STORAGE)) {
            raw = path.substring(STORAGE.length());
        }
        return raw == null ? null : normalize(raw);
    }

    /**
     * Материал из имени блока руды: silver_ore, deepslate_silver_ore, nether_silver_ore, ore_silver,
     * deepslate_ore_silver, silver_ore_nether. Ступени, плиты и прочие "silver_ore_что-то" не считаются.
     */
    @Nullable
    public static String materialOfOreId(String path) {
        String p;
        if (path.endsWith("_ore")) {
            p = stripStone(path.substring(0, path.length() - 4));
        } else if (path.startsWith("ore_")) {
            p = stripStone(path.substring(4));
        } else if (path.contains("_ore_")) {
            int at = path.indexOf("_ore_");
            String before = stripStone(path.substring(0, at));
            String after = stripStone(path.substring(at + 5));
            if (isStone(before)) {
                p = after;          // deepslate_ore_silver
            } else if (isStone(after)) {
                p = before;         // silver_ore_nether
            } else {
                return null;        // silver_ore_stairs и подобное
            }
        } else {
            return null;
        }
        // "deepslate_ore" и подобные - это порода, а не материал
        return isStone(p) || p.equals("ore") ? null : normalize(p);
    }

    /** Отрезает слова породы спереди и сзади: deepslate_silver -> silver, silver_nether -> silver. */
    private static String stripStone(String name) {
        String p = name;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String stone : STONE_PREFIXES) {
                if (p.startsWith(stone + "_")) {
                    p = p.substring(stone.length() + 1);
                    changed = true;
                } else if (p.endsWith("_" + stone)) {
                    p = p.substring(0, p.length() - stone.length() - 1);
                    changed = true;
                }
            }
        }
        return p;
    }

    private static boolean isStone(String name) {
        return name.isEmpty() || STONE_PREFIXES.contains(name);
    }

    /** Материал из имени блока хранения: silver_block, raw_silver_block, block_of_silver. */
    @Nullable
    static String materialOfStorageId(String path) {
        String p = null;
        if (path.startsWith("raw_") && path.endsWith("_block")) {
            p = path.substring(4, path.length() - 6);
        } else if (path.endsWith("_block")) {
            p = path.substring(0, path.length() - 6);
        } else if (path.startsWith("block_of_")) {
            p = path.substring(9);
        }
        return p == null || p.isEmpty() ? null : normalize(p);
    }

    static String normalize(String raw) {
        String m = raw.toLowerCase(Locale.ROOT);
        int slash = m.indexOf('/');
        if (slash > 0) {
            m = m.substring(0, slash);
        }
        return ALIASES.getOrDefault(m, m);
    }

    // ---------------------------------------------------------------- поиск

    private static Map<String, MaterialInfo> scan() {
        Set<String> blacklist = readBlacklist();
        Map<String, LinkedHashSet<Item>> ores = new TreeMap<>();
        Map<String, Set<String>> sources = new TreeMap<>();

        // 1. теги предметов
        for (TagKey<Item> tag : BuiltInRegistries.ITEM.getTagNames().toList()) {
            String m = materialOfTag(tag.location(), false);
            if (m == null) {
                continue;
            }
            Optional<HolderSet.Named<Item>> set = BuiltInRegistries.ITEM.getTag(tag);
            if (set.isPresent() && set.get().size() > 0) {
                set.get().forEach(h -> add(ores, m, h.value()));
                sources.computeIfAbsent(m, k -> new TreeSet<>()).add("#" + tag.location());
            }
        }
        // 2. теги блоков (некоторые моды вешают тег только на блок)
        for (TagKey<Block> tag : BuiltInRegistries.BLOCK.getTagNames().toList()) {
            String m = materialOfTag(tag.location(), false);
            if (m == null) {
                continue;
            }
            Optional<HolderSet.Named<Block>> set = BuiltInRegistries.BLOCK.getTag(tag);
            if (set.isPresent() && set.get().size() > 0) {
                set.get().forEach(h -> add(ores, m, h.value().asItem()));
                sources.computeIfAbsent(m, k -> new TreeSet<>()).add("#" + tag.location() + " (block)");
            }
        }
        // 3. имена блоков: silver_ore, deepslate_silver_ore и т. п.
        // Руда, уже найденная по тегу, второй раз не добавляется; блоки-машины (с BlockEntity) рудой не бывают.
        Set<Item> tagged = new HashSet<>();
        ores.values().forEach(tagged::addAll);
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            String m = materialOfOreId(id.getPath());
            if (m != null && block.asItem() != Items.AIR && !(block instanceof EntityBlock) && !tagged.contains(block.asItem())) {
                add(ores, m, block.asItem());
                sources.computeIfAbsent(m, k -> new TreeSet<>()).add("name " + id);
            }
        }
        // 4. ручные привязки из конфига: "modid:item=material"
        for (String entry : readList(true)) {
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(entry.substring(0, eq).trim());
            String m = normalize(entry.substring(eq + 1).trim());
            if (id != null && !m.isEmpty() && BuiltInRegistries.ITEM.containsKey(id)) {
                add(ores, m, BuiltInRegistries.ITEM.get(id));
                sources.computeIfAbsent(m, k -> new TreeSet<>()).add("config " + id);
            }
        }

        Lookup lookup = Lookup.current();
        Map<String, MaterialInfo> found = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<Item>> e : ores.entrySet()) {
            String m = e.getKey();
            if (blacklist.contains(m) || e.getValue().isEmpty()) {
                continue;
            }
            List<Item> oreList = new ArrayList<>(e.getValue());
            oreList.sort(Comparator.comparingInt((Item i) -> oreRank(i)).thenComparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
            String[] how = new String[1];
            Item product = findProduct(m, oreList, lookup, how);
            found.put(m, new MaterialInfo(m, product, List.copyOf(oreList), Collections.unmodifiableSet(sources.get(m)), how[0]));
        }
        if (!found.isEmpty()) {
            AdaptiveApiary.LOGGER.info("Adaptive Apiary: found {} ore materials{}: {}", found.size(),
                    lookup == null ? " (recipes not loaded yet)" : "", found.keySet());
        }
        return Collections.unmodifiableMap(found);
    }

    private static void add(Map<String, LinkedHashSet<Item>> ores, String material, Item item) {
        if (item != Items.AIR) {
            ores.computeIfAbsent(material, k -> new LinkedHashSet<>()).add(item);
        }
    }

    /** Порядок руд: сначала обычная каменная, потом глубинная, адская и прочие. */
    private static int oreRank(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if ("minecraft".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
            return path.startsWith("deepslate_") ? 1 : 0;
        }
        for (String prefix : STONE_PREFIXES) {
            if (path.startsWith(prefix + "_")) {
                return 3;
            }
        }
        return 2;
    }

    private static Item findProduct(String material, List<Item> ores, @Nullable Lookup lookup, String[] how) {
        // руды без своих тегов продукта
        if ("coal".equals(material)) {
            how[0] = "vanilla";
            return Items.COAL;
        }
        if ("netherite_scrap".equals(material)) {
            how[0] = "vanilla";
            return Items.NETHERITE_SCRAP;
        }
        Set<String> oreNamespaces = new HashSet<>();
        for (Item ore : ores) {
            oreNamespaces.add(BuiltInRegistries.ITEM.getKey(ore).getNamespace());
        }
        for (String prefix : PRODUCT_TAGS) {
            for (String ns : TAG_NAMESPACES) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ns, prefix + material));
                Optional<HolderSet.Named<Item>> items = BuiltInRegistries.ITEM.getTag(tag);
                if (items.isPresent() && items.get().size() > 0) {
                    how[0] = "#" + tag.location();
                    return pickPreferred(items.get().stream().map(h -> h.value()).toList(), oreNamespaces);
                }
            }
        }
        // результат переплавки руды
        if (lookup != null) {
            for (Item ore : ores) {
                Item smelted = lookup.smelt(ore);
                if (smelted != null && smelted != ore && !ores.contains(smelted)) {
                    how[0] = "smelting " + BuiltInRegistries.ITEM.getKey(ore);
                    return smelted;
                }
            }
        }
        // предмет с подходящим именем: сначала в модах руды, потом в любом моде
        List<String> names = List.of("raw_" + material, material, material + "_gem", material + "_ingot",
                material + "_crystal", material + "_dust", material + "_shard", "crushed_" + material);
        List<String> namespaces = new ArrayList<>(oreNamespaces);
        Collections.sort(namespaces);
        for (String path : names) {
            for (String ns : namespaces) {
                Item item = itemOrNull(ns, path);
                if (item != null && !ores.contains(item)) {
                    how[0] = "name " + ns + ":" + path;
                    return item;
                }
            }
        }
        for (String path : names) {
            List<Item> candidates = new ArrayList<>();
            for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
                if (id.getPath().equals(path)) {
                    candidates.add(BuiltInRegistries.ITEM.get(id));
                }
            }
            candidates.removeAll(ores);
            if (!candidates.isEmpty()) {
                Item item = pickPreferred(candidates, oreNamespaces);
                how[0] = "name " + BuiltInRegistries.ITEM.getKey(item);
                return item;
            }
        }
        // ничего не нашлось: соты дают саму руду
        how[0] = "ore itself";
        return ores.get(0);
    }

    @Nullable
    private static Item itemOrNull(String namespace, String path) {
        ResourceLocation id = ResourceLocation.tryParse(namespace + ":" + path);
        return id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
    }

    /** Из нескольких одинаковых предметов разных модов выбираем ванильный, затем из мода руды, затем первый по id. */
    private static Item pickPreferred(List<Item> items, Set<String> oreNamespaces) {
        Comparator<Item> order = Comparator
                .comparingInt((Item item) -> rank(BuiltInRegistries.ITEM.getKey(item).getNamespace(), oreNamespaces))
                .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString());
        return items.stream().min(order).orElseThrow();
    }

    private static int rank(String namespace, Set<String> oreNamespaces) {
        if ("minecraft".equals(namespace)) {
            return 0;
        }
        return oreNamespaces.contains(namespace) ? 1 : 2;
    }

    private static Set<String> readBlacklist() {
        Set<String> result = new HashSet<>();
        for (String entry : readList(false)) {
            result.add(normalize(entry.trim()));
        }
        return result;
    }

    private static List<? extends String> readList(boolean extraOres) {
        try {
            return extraOres ? Config.EXTRA_ORES.get() : Config.MATERIAL_BLACKLIST.get();
        } catch (IllegalStateException e) {
            // конфиг ещё не загружен
            return List.of();
        }
    }

    static String prettify(String material) {
        StringBuilder out = new StringBuilder();
        for (String word : material.replace('/', '_').split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** Доступ к рецептам переплавки: на сервере - рецепты сервера, на клиенте - присланные клиенту. */
    private record Lookup(RecipeManager recipes, HolderLookup.Provider registries) {
        @Nullable
        static Lookup current() {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                return new Lookup(server.getRecipeManager(), server.registryAccess());
            }
            Supplier<RecipeManager> recipes = clientRecipes;
            Supplier<HolderLookup.Provider> registries = clientRegistries;
            if (recipes == null || registries == null) {
                return null;
            }
            RecipeManager manager = recipes.get();
            HolderLookup.Provider provider = registries.get();
            return manager == null || provider == null ? null : new Lookup(manager, provider);
        }

        @Nullable
        Item smelt(Item ore) {
            ItemStack stack = new ItemStack(ore);
            Item result = cook(RecipeType.SMELTING, stack);
            return result != null ? result : cook(RecipeType.BLASTING, stack);
        }

        @Nullable
        private <T extends AbstractCookingRecipe> Item cook(RecipeType<T> type, ItemStack stack) {
            for (RecipeHolder<T> holder : recipes.getAllRecipesFor(type)) {
                List<Ingredient> ingredients = holder.value().getIngredients();
                if (!ingredients.isEmpty() && ingredients.get(0).test(stack)) {
                    ItemStack out = holder.value().getResultItem(registries);
                    if (!out.isEmpty()) {
                        return out.getItem();
                    }
                }
            }
            return null;
        }
    }

    private Materials() {}
}
