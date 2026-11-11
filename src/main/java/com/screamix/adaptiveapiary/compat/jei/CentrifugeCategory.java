package com.screamix.adaptiveapiary.compat.jei;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.blockentity.CentrifugeRecipes;
import com.screamix.adaptiveapiary.registry.ModItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Центрифуга: сота слева, справа всё, что из неё может выпасть, с шансами. */
public class CentrifugeCategory implements IRecipeCategory<CentrifugeRecipes.Display> {
    public static final RecipeType<CentrifugeRecipes.Display> TYPE =
            new RecipeType<>(AdaptiveApiary.id("centrifuge"), CentrifugeRecipes.Display.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public CentrifugeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(JeiTextures.TEXTURE, 0, 0, 116, 56);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModItems.CENTRIFUGE.get()));
        this.arrow = helper.createAnimatedDrawable(helper.createDrawable(JeiTextures.TEXTURE, 0, 60, 24, 16), 60,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<CentrifugeRecipes.Display> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.adaptiveapiary.centrifuge");
    }

    @Override
    public int getWidth() {
        return 116;
    }

    @Override
    public int getHeight() {
        return 56;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CentrifugeRecipes.Display recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 20).addItemStack(recipe.input());
        for (int i = 0; i < recipe.outputs().size() && i < 9; i++) {
            CentrifugeRecipes.Shown out = recipe.outputs().get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 63 + (i % 3) * 18, 2 + (i / 3) * 18)
                    .addItemStack(out.stack())
                    .addRichTooltipCallback(JeiTextures.chance(out.chance(), "jei.adaptiveapiary.chance"));
        }
    }

    @Override
    public void draw(CentrifugeRecipes.Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);
        arrow.draw(graphics, 25, 20);
    }
}
