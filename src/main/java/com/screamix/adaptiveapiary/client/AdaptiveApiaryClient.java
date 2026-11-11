package com.screamix.adaptiveapiary.client;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.genetics.BeeSpecies;
import com.screamix.adaptiveapiary.genetics.Materials;
import com.screamix.adaptiveapiary.item.BeeItem;
import com.screamix.adaptiveapiary.item.CombItem;
import com.screamix.adaptiveapiary.item.ResourceCombItem;
import com.screamix.adaptiveapiary.registry.ModItems;
import com.screamix.adaptiveapiary.registry.ModMenus;
import com.screamix.adaptiveapiary.registry.ModParticles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/** Клиентская часть: экраны и окраска предметов. Загружается только на клиенте. */
@Mod(value = AdaptiveApiary.MODID, dist = Dist.CLIENT)
public class AdaptiveApiaryClient {
    private static final int WHITE = 0xFFFFFFFF;

    public AdaptiveApiaryClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(AdaptiveApiaryClient::registerScreens);
        modBus.addListener(AdaptiveApiaryClient::registerItemColors);
        modBus.addListener(AdaptiveApiaryClient::registerParticles);
        NeoForge.EVENT_BUS.addListener(AdaptiveApiaryClient::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(AdaptiveApiaryClient::onRecipesUpdated);
        // на чужом сервере рецепты переплавки берутся из тех, что сервер прислал клиенту
        Materials.setClientSources(
                () -> {
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();
                    return connection == null ? null : connection.getRecipeManager();
                },
                () -> {
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();
                    return connection == null ? null : connection.registryAccess();
                });
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.APIARY.get(), ApiaryScreen::new);
        event.register(ModMenus.CENTRIFUGE.get(), CentrifugeScreen::new);
        event.register(ModMenus.BEE_BESTIARY.get(), BeeBestiaryScreen::new);
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BEE.get(), BeeParticle.Provider::new);
    }

    private static void onRecipesUpdated(RecipesUpdatedEvent event) {
        Materials.invalidate();
        MaterialColors.clear();
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Пчёлы: слой 0 - контур (основной цвет вида), слой 1 - тело (второй цвет),
        // слой 2 - голова, полосы, крылья и корона без окраски.
        event.register(AdaptiveApiaryClient::beeColor,
                ModItems.BEE_DRONE.get(), ModItems.BEE_PRINCESS.get(), ModItems.BEE_QUEEN.get());

        // Соты: слой 0 - мёд, слой 1 - стенки, слой 2 - блики и контур без окраски.
        for (var comb : ModItems.COMBS) {
            CombItem item = comb.get();
            event.register((stack, layer) -> layer > 1 ? WHITE
                    : opaque(layer == 0 ? item.primaryColor() : item.secondaryColor()), item);
        }
        event.register(AdaptiveApiaryClient::resourceCombColor, ModItems.RESOURCE_COMB.get());
    }

    private static int beeColor(ItemStack stack, int layer) {
        if (layer > 1) {
            return WHITE;
        }
        BeeSpecies species = BeeItem.genome(stack).activeSpecies();
        return opaque(layer == 0 ? BeeColors.outline(species) : BeeColors.body(species));
    }

    private static int resourceCombColor(ItemStack stack, int layer) {
        if (layer > 1) {
            return WHITE;
        }
        String material = ResourceCombItem.material(stack);
        // стенки в текстуре уже темнее мёда, поэтому оба слоя красятся одним цветом материала
        int color = material.isEmpty() ? 0xB8B8B8 : MaterialColors.get(material);
        return opaque(color);
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    // Теги и набор модов могли измениться: цвета материалов считаем заново.
    private static void onTagsUpdated(TagsUpdatedEvent event) {
        MaterialColors.clear();
    }
}
