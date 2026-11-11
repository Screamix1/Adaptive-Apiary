package com.screamix.adaptiveapiary.registry;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.menu.ApiaryMenu;
import com.screamix.adaptiveapiary.menu.BeeBestiaryMenu;
import com.screamix.adaptiveapiary.menu.CentrifugeMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AdaptiveApiary.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ApiaryMenu>> APIARY =
            MENUS.register("apiary", () -> IMenuTypeExtension.create(ApiaryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CentrifugeMenu>> CENTRIFUGE =
            MENUS.register("centrifuge", () -> IMenuTypeExtension.create(CentrifugeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BeeBestiaryMenu>> BEE_BESTIARY =
            MENUS.register("bee_bestiary", () -> IMenuTypeExtension.create(BeeBestiaryMenu::new));

    private ModMenus() {}
}
