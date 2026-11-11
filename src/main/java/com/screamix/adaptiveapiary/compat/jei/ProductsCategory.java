package com.screamix.adaptiveapiary.compat.jei;

import java.util.List;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.genetics.BeeGenome;
import com.screamix.adaptiveapiary.genetics.BeeSpecies;
import com.screamix.adaptiveapiary.genetics.Traits;
import com.screamix.adaptiveapiary.item.BeeItem;
import com.screamix.adaptiveapiary.item.BeeType;
import com.screamix.adaptiveapiary.registry.ModItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Что приносит пчела: королева вида и её продукты с шансом за рабочий цикл. */
public class ProductsCategory implements IRecipeCategory<BeeSpecies> {
    public static final RecipeType<BeeSpecies> TYPE = new RecipeType<>(AdaptiveApiary.id("bee_products"), BeeSpecies.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ProductsCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(JeiTextures.TEXTURE, 0, 80, 140, 20);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModItems.HONEY_COMB.get()));
    }

    static ItemStack bee(BeeType type, BeeSpecies species) {
        return BeeItem.create(type, BeeGenome.pure(species).withAnalyzed(true));
    }

    static List<ItemStack> allTypes(BeeSpecies species) {
        return List.of(bee(BeeType.QUEEN, species), bee(BeeType.PRINCESS, species), bee(BeeType.DRONE, species));
    }

    @Override
    public RecipeType<BeeSpecies> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.adaptiveapiary.products");
    }

    @Override
    public int getWidth() {
        return 140;
    }

    @Override
    public int getHeight() {
        return 40;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BeeSpecies species, IFocusGroup focuses) {
        // любая пчела этого вида (трутень, принцесса, королева) ведёт к этому рецепту
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(allTypes(species));
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 2).addItemStack(bee(BeeType.QUEEN, species));
        float speed = Traits.speedMultiplier(species.speed());
        List<BeeSpecies.Product> products = species.products();
        for (int i = 0; i < products.size() && i < 4; i++) {
            BeeSpecies.Product product = products.get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 57 + i * 21, 2)
                    .addItemStack(product.stack().get())
                    .addRichTooltipCallback(JeiTextures.chance(Math.min(1.0F, product.chance() * speed), "jei.adaptiveapiary.chance_per_cycle"));
        }
    }

    @Override
    public void draw(BeeSpecies species, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);
        float speed = Traits.speedMultiplier(species.speed());
        List<BeeSpecies.Product> products = species.products();
        for (int i = 0; i < products.size() && i < 4; i++) {
            JeiTextures.centered(graphics, Component.literal(JeiTextures.percent(Math.min(1.0F, products.get(i).chance() * speed))),
                    57 + i * 21 + 8, 22, JeiTextures.TEXT);
        }
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, species.displayName(), 0, 31, JeiTextures.TEXT, false);
    }
}
