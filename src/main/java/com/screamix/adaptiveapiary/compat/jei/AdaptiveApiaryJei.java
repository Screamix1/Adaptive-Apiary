package com.screamix.adaptiveapiary.compat.jei;

import java.util.List;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.blockentity.CentrifugeRecipes;
import com.screamix.adaptiveapiary.genetics.BeeSpecies;
import com.screamix.adaptiveapiary.genetics.SpeciesRegistry;
import com.screamix.adaptiveapiary.item.BeeItem;
import com.screamix.adaptiveapiary.item.ResourceCombItem;
import com.screamix.adaptiveapiary.registry.ModBlocks;
import com.screamix.adaptiveapiary.registry.ModItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Поддержка JEI. Класс загружается только самим JEI, поэтому без JEI мод работает как обычно. */
@JeiPlugin
public class AdaptiveApiaryJei implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return AdaptiveApiary.id("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // каждая порода пчёл и каждый материал рудных сот - отдельный предмет в списке JEI
        ISubtypeInterpreter<ItemStack> species = new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return BeeItem.genome(stack).species().toString();
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return BeeItem.genome(stack).species().toString();
            }
        };
        registration.registerSubtypeInterpreter(ModItems.BEE_DRONE.get(), species);
        registration.registerSubtypeInterpreter(ModItems.BEE_PRINCESS.get(), species);
        registration.registerSubtypeInterpreter(ModItems.BEE_QUEEN.get(), species);
        registration.registerSubtypeInterpreter(ModItems.RESOURCE_COMB.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return ResourceCombItem.material(stack);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return ResourceCombItem.material(stack);
            }
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CentrifugeCategory(helper), new ProductsCategory(helper), new MutationsCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CentrifugeCategory.TYPE, CentrifugeRecipes.displays());
        List<BeeSpecies> species = SpeciesRegistry.allKnown().stream().filter(s -> !s.products().isEmpty()).toList();
        registration.addRecipes(ProductsCategory.TYPE, species);
        registration.addRecipes(MutationsCategory.TYPE, MutationsCategory.displays());

        info(registration, ModItems.SCOOP.get().getDefaultInstance(), "scoop");
        info(registration, ModItems.FRAME.get().getDefaultInstance(), "frame");
        info(registration, ModItems.APIARY.get().getDefaultInstance(), "apiary");
        info(registration, ModItems.BEEALYZER.get().getDefaultInstance(), "beealyzer");
        for (var hive : ModBlocks.HIVES) {
            info(registration, new ItemStack(hive.get()), "hive");
        }
    }

    private static void info(IRecipeRegistration registration, ItemStack stack, String key) {
        registration.addIngredientInfo(stack, VanillaTypes.ITEM_STACK, Component.translatable("jei.adaptiveapiary.info." + key));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.CENTRIFUGE.get()), CentrifugeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.APIARY.get()), ProductsCategory.TYPE, MutationsCategory.TYPE);
    }
}
