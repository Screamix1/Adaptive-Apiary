package com.screamix.adaptiveapiary.genetics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.item.BeeItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class BeeDiscoveries {
    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AdaptiveApiary.MODID);

    private static final Supplier<AttachmentType<Set<ResourceLocation>>> SPECIES =
            TYPES.register("discovered_bees", () -> AttachmentType.<Set<ResourceLocation>>builder(() -> Set.of())
                    .serialize(ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf), species -> !species.isEmpty())
                    .copyOnDeath()
                    .build());

    /** Запоминает оба вида изученной особи, в том числе у пчёл из старых сохранений. */
    public static Set<ResourceLocation> collect(ServerPlayer player) {
        Set<ResourceLocation> saved = player.getData(SPECIES);
        Set<ResourceLocation> discovered = new HashSet<>(saved);
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && BeeItem.typeOf(stack) != null) {
                BeeGenome genome = BeeItem.genome(stack);
                if (genome.analyzed()) {
                    discovered.add(genome.species());
                    discovered.add(genome.inactiveSpecies());
                }
            }
        }
        if (discovered.size() == saved.size()) {
            return saved;
        }
        Set<ResourceLocation> updated = Set.copyOf(discovered);
        player.setData(SPECIES, updated);
        return updated;
    }

    private BeeDiscoveries() {}
}
