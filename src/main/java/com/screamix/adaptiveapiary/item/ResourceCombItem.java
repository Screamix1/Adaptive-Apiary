package com.screamix.adaptiveapiary.item;

import java.util.List;

import com.screamix.adaptiveapiary.genetics.Materials;
import com.screamix.adaptiveapiary.registry.ModComponents;
import com.screamix.adaptiveapiary.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Рудные соты. Один предмет на все материалы: имя материала лежит в компоненте данных. */
public class ResourceCombItem extends Item {
    public ResourceCombItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(String material) {
        ItemStack stack = new ItemStack(ModItems.RESOURCE_COMB.get());
        stack.set(ModComponents.MATERIAL.get(), material);
        return stack;
    }

    /** Материал сот или пустая строка, если компонента нет. */
    public static String material(ItemStack stack) {
        String material = stack.get(ModComponents.MATERIAL.get());
        return material == null ? "" : material;
    }

    @Override
    public Component getName(ItemStack stack) {
        String material = material(stack);
        if (material.isEmpty()) {
            return super.getName(stack);
        }
        return Component.translatable("item.adaptiveapiary.resource_comb.named", Materials.displayName(material));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String material = material(stack);
        if (material.isEmpty()) {
            return;
        }
        ItemStack product = Materials.product(material);
        if (product.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.adaptiveapiary.comb_no_product").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.adaptiveapiary.comb_product", product.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
