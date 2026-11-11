package com.screamix.adaptiveapiary.compat.jei;

import com.screamix.adaptiveapiary.AdaptiveApiary;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Общее для категорий JEI: текстура фонов, подписи и подсказка с шансом. */
final class JeiTextures {
    static final ResourceLocation TEXTURE = AdaptiveApiary.id("textures/gui/jei.png");
    static final int TEXT = 0x404040;

    static String percent(float chance) {
        float p = chance * 100.0F;
        return (p >= 10.0F || p == Math.round(p) ? String.valueOf(Math.round(p)) : String.format(java.util.Locale.ROOT, "%.1f", p)) + "%";
    }

    static IRecipeSlotRichTooltipCallback chance(float chance, String key) {
        return (view, tooltip) -> addChance(tooltip, chance, key);
    }

    static void addChance(ITooltipBuilder tooltip, float chance, String key) {
        tooltip.add(Component.translatable(key, percent(chance)).withStyle(ChatFormatting.GOLD));
    }

    static void centered(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private JeiTextures() {}
}
