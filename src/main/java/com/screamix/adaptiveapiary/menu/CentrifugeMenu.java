package com.screamix.adaptiveapiary.menu;

import javax.annotation.Nullable;

import com.screamix.adaptiveapiary.blockentity.CentrifugeBlockEntity;
import com.screamix.adaptiveapiary.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CentrifugeMenu extends MachineMenu {
    private final ContainerData data;

    /** Клиентская сторона. */
    public CentrifugeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, CentrifugeBlockEntity.createInventory(null), new SimpleContainerData(1), null);
    }

    public CentrifugeMenu(int containerId, Inventory playerInventory, IItemHandler inventory, ContainerData data,
            @Nullable BlockEntity blockEntity) {
        super(ModMenus.CENTRIFUGE.get(), containerId, blockEntity, CentrifugeBlockEntity.SLOTS, CentrifugeBlockEntity.OUTPUT_START);
        this.data = data;

        addSlot(new SlotItemHandler(inventory, CentrifugeBlockEntity.SLOT_INPUT, 35, 35));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new OutputSlot(inventory, CentrifugeBlockEntity.OUTPUT_START + row * 3 + column, 98 + column * 18, 17 + row * 18));
            }
        }
        addPlayerSlots(playerInventory, 8, 94);
        addDataSlots(data);
    }

    /** Прогресс обработки, 0..100. */
    public int progress() {
        return data.get(0);
    }
}
