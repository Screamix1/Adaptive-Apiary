package com.screamix.adaptiveapiary.blockentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.screamix.adaptiveapiary.Config;
import com.screamix.adaptiveapiary.genetics.Materials;
import com.screamix.adaptiveapiary.item.ResourceCombItem;
import com.screamix.adaptiveapiary.registry.ModItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Что центрифуга достаёт из сот. Рудные соты обрабатываются отдельно: их продукт зависит от материала. */
public final class CentrifugeRecipes {
    /** Один возможный выход: предмет и шанс его получить. */
    public record Output(Supplier<Item> item, float chance) {}

    private static Map<Item, List<Output>> recipes;

    private static Map<Item, List<Output>> recipes() {
        if (recipes == null) {
            Map<Item, List<Output>> map = new HashMap<>();
            map.put(ModItems.HONEY_COMB.get(), List.of(out(ModItems.BEESWAX, 1.0F), out(ModItems.HONEY_DROP, 0.9F)));
            map.put(ModItems.DRIPPING_COMB.get(), List.of(out(ModItems.HONEYDEW, 1.0F), out(ModItems.HONEY_DROP, 0.4F)));
            map.put(ModItems.STRINGY_COMB.get(), List.of(out(ModItems.PROPOLIS, 1.0F), out(ModItems.HONEY_DROP, 0.4F)));
            map.put(ModItems.PARCHED_COMB.get(), List.of(out(ModItems.BEESWAX, 1.0F), out(ModItems.HONEY_DROP, 0.9F)));
            map.put(ModItems.SILKY_COMB.get(), List.of(out(ModItems.HONEY_DROP, 1.0F), out(() -> Items.STRING, 0.5F)));
            map.put(ModItems.FROZEN_COMB.get(), List.of(out(ModItems.BEESWAX, 0.8F), out(ModItems.HONEY_DROP, 0.7F), out(() -> Items.SNOWBALL, 0.4F)));
            map.put(ModItems.MOSSY_COMB.get(), List.of(out(ModItems.BEESWAX, 1.0F), out(ModItems.HONEY_DROP, 0.9F), out(() -> Items.SLIME_BALL, 0.15F)));
            map.put(ModItems.ROCKY_COMB.get(), List.of(out(ModItems.BEESWAX, 0.5F), out(() -> Items.FLINT, 0.3F)));
            recipes = map;
        }
        return recipes;
    }

    private static Output out(Supplier<? extends Item> item, float chance) {
        return new Output(item::get, chance);
    }

    /** Что показать в JEI: вход и все возможные выходы с шансами. */
    public record Display(ItemStack input, List<Shown> outputs) {}

    public record Shown(ItemStack stack, float chance) {}

    public static List<Display> displays() {
        List<Display> list = new ArrayList<>();
        for (Map.Entry<Item, List<Output>> e : recipes().entrySet()) {
            List<Shown> outs = new ArrayList<>();
            for (Output o : e.getValue()) {
                outs.add(new Shown(new ItemStack(o.item().get()), o.chance()));
            }
            list.add(new Display(new ItemStack(e.getKey()), outs));
        }
        float yield;
        try {
            yield = Config.RESOURCE_YIELD.get().floatValue();
        } catch (IllegalStateException e) {
            yield = 0.25F;
        }
        for (String material : Materials.all().keySet()) {
            list.add(new Display(ResourceCombItem.create(material), List.of(
                    new Shown(Materials.product(material), yield),
                    new Shown(new ItemStack(ModItems.BEESWAX.get()), 0.5F))));
        }
        return list;
    }

    public static boolean canProcess(ItemStack input) {
        if (input.isEmpty()) {
            return false;
        }
        if (input.getItem() instanceof ResourceCombItem) {
            return Materials.exists(ResourceCombItem.material(input));
        }
        return recipes().containsKey(input.getItem());
    }

    /** Бросает кубики и возвращает то, что получилось из одной соты. */
    public static List<ItemStack> roll(ItemStack input, RandomSource random) {
        List<ItemStack> result = new ArrayList<>();
        if (input.getItem() instanceof ResourceCombItem) {
            if (random.nextFloat() < 0.5F) {
                result.add(new ItemStack(ModItems.BEESWAX.get()));
            }
            ItemStack product = Materials.product(ResourceCombItem.material(input));
            if (!product.isEmpty() && random.nextDouble() < Config.RESOURCE_YIELD.get()) {
                result.add(product);
            }
            return result;
        }
        List<Output> outputs = recipes().get(input.getItem());
        if (outputs != null) {
            for (Output output : outputs) {
                if (random.nextFloat() < output.chance()) {
                    result.add(new ItemStack(output.item().get()));
                }
            }
        }
        return result;
    }

    private CentrifugeRecipes() {}
}
