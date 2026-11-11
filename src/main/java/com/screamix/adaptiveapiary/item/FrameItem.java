package com.screamix.adaptiveapiary.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Рамка для пасеки: каждая увеличивает производство и изнашивается на 1 за рабочий цикл. */
public class FrameItem extends Item {
    /** Прибавка к производству от одной рамки. */
    public static final float BONUS = 0.5F;

    public FrameItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.adaptiveapiary.frame", Math.round(BONUS * 100)).withStyle(ChatFormatting.GRAY));
    }
}
