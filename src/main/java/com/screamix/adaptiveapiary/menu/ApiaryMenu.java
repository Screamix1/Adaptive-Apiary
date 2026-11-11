package com.screamix.adaptiveapiary.menu;

import javax.annotation.Nullable;

import com.screamix.adaptiveapiary.blockentity.ApiaryBlockEntity;
import com.screamix.adaptiveapiary.blockentity.ApiaryStatus;
import com.screamix.adaptiveapiary.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ApiaryMenu extends MachineMenu {
    /** Координаты семи выходных слотов: соты вокруг центра. */
    private static final int[][] OUTPUT_POSITIONS = {
            {116, 38}, {116, 16}, {116, 60}, {95, 27}, {137, 27}, {95, 49}, {137, 49}
    };

    private final ContainerData data;

    /** Клиентская сторона: настоящего блока нет, инвентарь заполняется пакетами с сервера. */
    public ApiaryMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, ApiaryBlockEntity.createInventory(null),
                new SimpleContainerData(ApiaryBlockEntity.DATA_COUNT), null);
    }

    public ApiaryMenu(int containerId, Inventory playerInventory, IItemHandler inventory, ContainerData data,
            @Nullable BlockEntity blockEntity) {
        super(ModMenus.APIARY.get(), containerId, blockEntity, ApiaryBlockEntity.SLOTS, ApiaryBlockEntity.FRAMES_END);
        this.data = data;

        addSlot(new SlotItemHandler(inventory, ApiaryBlockEntity.SLOT_QUEEN, 30, 26));
        addSlot(new SlotItemHandler(inventory, ApiaryBlockEntity.SLOT_DRONE, 30, 52));
        for (int i = 0; i < 3; i++) {
            addSlot(new SlotItemHandler(inventory, ApiaryBlockEntity.FRAMES_START + i, 62, 19 + i * 20));
        }
        for (int i = 0; i < OUTPUT_POSITIONS.length; i++) {
            addSlot(new OutputSlot(inventory, ApiaryBlockEntity.OUTPUT_START + i, OUTPUT_POSITIONS[i][0], OUTPUT_POSITIONS[i][1]));
        }
        addPlayerSlots(playerInventory, 8, 104);
        addDataSlots(data);
    }

    public ApiaryStatus status() {
        return ApiaryStatus.byId(data.get(ApiaryBlockEntity.DATA_STATUS));
    }

    /** Прогресс скрещивания или рабочего цикла, 0..100. */
    public int progress() {
        return data.get(ApiaryBlockEntity.DATA_PROGRESS);
    }

    /** Остаток жизни королевы, 0..100. */
    public int life() {
        return data.get(ApiaryBlockEntity.DATA_LIFE);
    }
}
