package com.screamix.adaptiveapiary.menu;

import java.util.Set;

import com.screamix.adaptiveapiary.registry.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class BeeBestiaryMenu extends AbstractContainerMenu {
    private final Set<ResourceLocation> discoveries;

    public BeeBestiaryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, Set.copyOf(buffer.readList(FriendlyByteBuf::readResourceLocation)));
    }

    public BeeBestiaryMenu(int id, Set<ResourceLocation> discoveries) {
        super(ModMenus.BEE_BESTIARY.get(), id);
        this.discoveries = discoveries;
    }

    public Set<ResourceLocation> discoveries() {
        return discoveries;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
