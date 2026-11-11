package com.screamix.adaptiveapiary.genetics;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Вид пчелы. Встроенные виды описаны в {@link SpeciesRegistry},
 * рудные виды создаются на лету по имени материала.
 *
 * @param primaryColor   цвет контура (RGB), для рудных видов не используется
 * @param secondaryColor цвет тела (RGB), для рудных видов не используется
 * @param caveDwelling   работает без неба, ночью и в дождь
 * @param material       имя материала для рудного вида, иначе null
 */
public record BeeSpecies(
        ResourceLocation id,
        int primaryColor,
        int secondaryColor,
        FlowerType flowers,
        boolean caveDwelling,
        int speed,
        int lifespan,
        int fertility,
        List<Product> products,
        @Nullable String material) {

    /** Продукт пчелы: стек и шанс получить его за один рабочий цикл при скорости 1.0. */
    public record Product(Supplier<ItemStack> stack, float chance) {}

    public boolean isOreSpecies() {
        return material != null;
    }

    public Component displayName() {
        if (material != null) {
            return Component.translatable("species.adaptiveapiary.ore", Materials.displayName(material));
        }
        return Component.translatable("species.adaptiveapiary." + id.getPath());
    }
}
