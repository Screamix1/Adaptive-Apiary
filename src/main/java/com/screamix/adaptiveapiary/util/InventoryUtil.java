package com.screamix.adaptiveapiary.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class InventoryUtil {
    /** Кладёт стек в слоты [from, to): сначала к таким же предметам, потом в пустые. Возвращает остаток. */
    public static ItemStack insert(ItemStackHandler handler, int from, int to, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = from; slot < to && !remaining.isEmpty(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                remaining = handler.insertItem(slot, remaining, false);
            }
        }
        for (int slot = from; slot < to && !remaining.isEmpty(); slot++) {
            if (handler.getStackInSlot(slot).isEmpty()) {
                remaining = handler.insertItem(slot, remaining, false);
            }
        }
        return remaining;
    }

    /** Пытается разложить отложенные предметы по слотам [from, to). Возвращает true, если очередь пуста. */
    public static boolean flush(List<ItemStack> pending, ItemStackHandler handler, int from, int to) {
        Iterator<ItemStack> iterator = pending.iterator();
        List<ItemStack> leftovers = new ArrayList<>();
        while (iterator.hasNext()) {
            ItemStack rest = insert(handler, from, to, iterator.next());
            iterator.remove();
            if (!rest.isEmpty()) {
                leftovers.add(rest);
            }
        }
        pending.addAll(leftovers);
        return pending.isEmpty();
    }

    public static ListTag save(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                list.add(stack.saveOptional(registries));
            }
        }
        return list;
    }

    public static List<ItemStack> load(CompoundTag tag, String key, HolderLookup.Provider registries) {
        List<ItemStack> stacks = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    public static void dropAll(Level level, BlockPos pos, IItemHandler handler, List<ItemStack> pending) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
        }
        for (ItemStack stack : pending) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
        }
    }

    private InventoryUtil() {}
}
