package com.screamix.adaptiveapiary.blockentity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.screamix.adaptiveapiary.Config;
import com.screamix.adaptiveapiary.menu.CentrifugeMenu;
import com.screamix.adaptiveapiary.registry.ModBlockEntities;
import com.screamix.adaptiveapiary.util.InventoryUtil;
import com.screamix.adaptiveapiary.util.RangedItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Центрифуга: разбирает соты на воск, мёд и ресурсы. Энергия не нужна. */
public class CentrifugeBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_END = 10;
    public static final int SLOTS = 10;

    private final ItemStackHandler inventory = createInventory(this::setChanged);
    private final IItemHandler automation = new RangedItemHandler(inventory, SLOT_INPUT, OUTPUT_START, OUTPUT_START, OUTPUT_END);
    private final List<ItemStack> pending = new ArrayList<>();
    private int progress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress * 100 / Math.max(1, Config.CENTRIFUGE_TICKS.get()) : 0;
        }

        @Override
        public void set(int index, int value) {
            // значение считается на сервере
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CENTRIFUGE.get(), pos, state);
    }

    public static ItemStackHandler createInventory(@Nullable Runnable onChange) {
        return new ItemStackHandler(SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot != SLOT_INPUT || CentrifugeRecipes.canProcess(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (onChange != null) {
                    onChange.run();
                }
            }
        };
    }

    public IItemHandler getAutomationHandler() {
        return automation;
    }

    public void dropContents() {
        if (level != null) {
            InventoryUtil.dropAll(level, worldPosition, inventory, pending);
            pending.clear();
        }
    }

    public void serverTick() {
        Level level = this.level;
        if (level == null) {
            return;
        }
        if (!pending.isEmpty()) {
            if (!InventoryUtil.flush(pending, inventory, OUTPUT_START, OUTPUT_END)) {
                return;
            }
            setChanged();
        }
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (!CentrifugeRecipes.canProcess(input)) {
            progress = 0;
            return;
        }
        if (++progress < Config.CENTRIFUGE_TICKS.get()) {
            return;
        }
        progress = 0;
        List<ItemStack> results = CentrifugeRecipes.roll(input, level.getRandom());
        inventory.extractItem(SLOT_INPUT, 1, false);
        for (ItemStack result : results) {
            ItemStack rest = InventoryUtil.insert(inventory, OUTPUT_START, OUTPUT_END, result);
            if (!rest.isEmpty()) {
                pending.add(rest);
            }
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.adaptiveapiary.centrifuge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CentrifugeMenu(containerId, playerInventory, inventory, data, this);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Pending", InventoryUtil.save(pending, registries));
        tag.putInt("Progress", progress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        pending.clear();
        pending.addAll(InventoryUtil.load(tag, "Pending", registries));
        progress = tag.getInt("Progress");
    }
}
