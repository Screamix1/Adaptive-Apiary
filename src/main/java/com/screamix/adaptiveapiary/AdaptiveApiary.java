package com.screamix.adaptiveapiary;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.screamix.adaptiveapiary.command.ApiaryCommand;
import com.screamix.adaptiveapiary.genetics.BeeDiscoveries;
import com.screamix.adaptiveapiary.genetics.Materials;
import com.screamix.adaptiveapiary.registry.ModBlockEntities;
import com.screamix.adaptiveapiary.registry.ModBlocks;
import com.screamix.adaptiveapiary.registry.ModComponents;
import com.screamix.adaptiveapiary.registry.ModItems;
import com.screamix.adaptiveapiary.registry.ModMenus;
import com.screamix.adaptiveapiary.registry.ModParticles;
import com.screamix.adaptiveapiary.registry.ModSounds;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod(AdaptiveApiary.MODID)
public class AdaptiveApiary {
    public static final String MODID = "adaptiveapiary";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AdaptiveApiary(IEventBus modBus, ModContainer container) {
        BeeDiscoveries.TYPES.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);

        modBus.addListener(AdaptiveApiary::registerCapabilities);
        modBus.addListener(AdaptiveApiary::onConfigChanged);
        NeoForge.EVENT_BUS.addListener(AdaptiveApiary::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(AdaptiveApiary::onServerStarted);
        NeoForge.EVENT_BUS.addListener(ApiaryCommand::register);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.APIARY.get(),
                (be, side) -> be.getAutomationHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CENTRIFUGE.get(),
                (be, side) -> be.getAutomationHandler());
    }

    // Теги приходят из датапаков и могут меняться по /reload: список материалов надо пересобрать.
    private static void onTagsUpdated(TagsUpdatedEvent event) {
        Materials.invalidate();
    }

    // К запуску сервера готовы рецепты: продукт руды теперь можно найти и по переплавке.
    private static void onServerStarted(ServerStartedEvent event) {
        Materials.invalidate();
    }

    // Чёрный список материалов читается из конфига.
    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            Materials.invalidate();
        }
    }
}
