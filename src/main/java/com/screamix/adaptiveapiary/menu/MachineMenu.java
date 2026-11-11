package com.screamix.adaptiveapiary.menu;

import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Общая часть меню: слоты игрока, shift-клик и проверка расстояния до блока. */
public abstract class MachineMenu extends AbstractContainerMenu {
    @Nullable
    private final BlockEntity blockEntity;
    private final int machineSlots;
    private final int inputSlots;

    /**
     * @param machineSlots сколько слотов у блока (они должны быть добавлены первыми)
     * @param inputSlots   сколько первых слотов блока принимают предметы по shift-клику
     */
    protected MachineMenu(MenuType<?> type, int containerId, @Nullable BlockEntity blockEntity, int machineSlots, int inputSlots) {
        super(type, containerId);
        this.blockEntity = blockEntity;
        this.machineSlots = machineSlots;
        this.inputSlots = inputSlots;
    }

    protected void addPlayerSlots(Inventory playerInventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, left + column * 18, top + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, inputSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        // слоты блока не сообщают блоку об изменении размера стека, поэтому помечаем его сами
        if (blockEntity != null) {
            blockEntity.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return true;
        }
        if (blockEntity.isRemoved()) {
            return false;
        }
        return player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0D;
    }
}
