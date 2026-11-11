package com.screamix.adaptiveapiary.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.genetics.BeeSpecies;
import com.screamix.adaptiveapiary.genetics.Mutations;
import com.screamix.adaptiveapiary.genetics.SpeciesRegistry;
import com.screamix.adaptiveapiary.item.BeeType;
import com.screamix.adaptiveapiary.registry.ModItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Мутации: принцесса + трутень -> новый вид, шанс и (для рудных пчёл) блок под пасекой. */
public class MutationsCategory implements IRecipeCategory<MutationsCategory.Display> {
    public static final RecipeType<Display> TYPE = new RecipeType<>(AdaptiveApiary.id("mutations"), Display.class);

    /** Готовая к показу мутация. condition пустой, если особых условий нет. */
    public record Display(List<BeeSpecies> first, List<BeeSpecies> second, BeeSpecies result, float chance, List<ItemStack> condition) {}

    private final IDrawable background;
    private final IDrawable conditionSlot;
    private final IDrawable icon;

    public MutationsCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(JeiTextures.TEXTURE, 0, 130, 120, 22);
        this.conditionSlot = helper.createDrawable(JeiTextures.TEXTURE, 62, 154, 18, 18);
        this.icon = helper.createDrawableItemStack(ProductsCategory.bee(BeeType.PRINCESS, SpeciesRegistry.get(SpeciesRegistry.IMPERIAL)));
    }

    public static List<Display> displays() {
        List<Display> list = new ArrayList<>();
        for (Mutations.Mutation m : Mutations.all()) {
            list.add(new Display(species(m.first()), species(m.second()), SpeciesRegistry.get(m.result()), m.chance(), List.of()));
        }
        for (Mutations.OreMutation m : Mutations.oreMutations()) {
            List<ItemStack> blocks = new ArrayList<>();
            for (Item item : m.blocks()) {
                blocks.add(new ItemStack(item));
            }
            list.add(new Display(List.of(SpeciesRegistry.get(SpeciesRegistry.MINERAL)), List.of(SpeciesRegistry.get(SpeciesRegistry.CULTIVATED)),
                    SpeciesRegistry.get(m.result()), m.chance(), blocks));
        }
        return list;
    }

    private static List<BeeSpecies> species(List<ResourceLocation> ids) {
        return ids.stream().map(SpeciesRegistry::get).toList();
    }

    private static List<ItemStack> stacks(List<BeeSpecies> species, BeeType type) {
        return species.stream().map(s -> ProductsCategory.bee(type, s)).toList();
    }

    @Override
    public RecipeType<Display> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.adaptiveapiary.mutations");
    }

    @Override
    public int getWidth() {
        return 140;
    }

    @Override
    public int getHeight() {
        return 44;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Display recipe, IFocusGroup focuses) {
        // поиск по любой стадии родителей и потомка
        var inputs = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        var outputs = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
        for (BeeType type : BeeType.values()) {
            inputs.addItemStacks(stacks(recipe.first(), type));
            inputs.addItemStacks(stacks(recipe.second(), type));
            outputs.addItemStack(ProductsCategory.bee(type, recipe.result()));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 3).addItemStacks(stacks(recipe.first(), BeeType.PRINCESS));
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 3).addItemStacks(stacks(recipe.second(), BeeType.DRONE));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 97, 3)
                .addItemStack(ProductsCategory.bee(BeeType.PRINCESS, recipe.result()))
                .addRichTooltipCallback(JeiTextures.chance(recipe.chance(), "jei.adaptiveapiary.mutation_chance"));
        if (!recipe.condition().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 63, 25).addItemStacks(recipe.condition())
                    .addRichTooltipCallback((view, tooltip) ->
                            tooltip.add(Component.translatable("jei.adaptiveapiary.under_apiary").withStyle(ChatFormatting.GOLD)));
        }
    }

    @Override
    public void draw(Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);
        JeiTextures.centered(graphics, Component.literal(JeiTextures.percent(recipe.chance())), 72, 0, JeiTextures.TEXT);
        if (!recipe.condition().isEmpty()) {
            conditionSlot.draw(graphics, 62, 24);
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                    Component.translatable("jei.adaptiveapiary.under_apiary_short"), 84, 29, JeiTextures.TEXT, false);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, Display recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= 58 && mouseX <= 86 && mouseY >= 0 && mouseY <= 20) {
            tooltip.add(Component.translatable("jei.adaptiveapiary.mutation_hint"));
        }
    }

    static ItemStack apiary() {
        return new ItemStack(ModItems.APIARY.get());
    }
}
