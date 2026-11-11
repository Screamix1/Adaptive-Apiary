package com.screamix.adaptiveapiary.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Вид на инвентарь для воронок и труб: класть можно только во входные слоты,
 * забирать только из выходных.
 */
public class RangedItemHandler implements IItemHandler {
    private final IItemHandler inner;
    private final int inputStart;
    private final int inputEnd;
    private final int outputStart;
    private final int outputEnd;

    /** Границы заданы как [start, end). */
    public RangedItemHandler(IItemHandler inner, int inputStart, int inputEnd, int outputStart, int outputEnd) {
        this.inner = inner;
        this.inputStart = inputStart;
        this.inputEnd = inputEnd;
        this.outputStart = outputStart;
        this.outputEnd = outputEnd;
    }

    private boolean isInput(int slot) {
        return slot >= inputStart && slot < inputEnd;
    }

    private boolean isOutput(int slot) {
        return slot >= outputStart && slot < outputEnd;
    }

    @Override
    public int getSlots() {
        return inner.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inner.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isInput(slot) ? inner.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return isOutput(slot) ? inner.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return inner.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isInput(slot) && inner.isItemValid(slot, stack);
    }
}
