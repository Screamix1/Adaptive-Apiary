package com.screamix.adaptiveapiary.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.screamix.adaptiveapiary.Config;
import com.screamix.adaptiveapiary.block.ApiaryBlock;
import com.screamix.adaptiveapiary.genetics.BeeBreeding;
import com.screamix.adaptiveapiary.genetics.BeeGenome;
import com.screamix.adaptiveapiary.genetics.BeeSpecies;
import com.screamix.adaptiveapiary.genetics.Traits;
import com.screamix.adaptiveapiary.item.BeeItem;
import com.screamix.adaptiveapiary.item.BeeType;
import com.screamix.adaptiveapiary.item.FrameItem;
import com.screamix.adaptiveapiary.menu.ApiaryMenu;
import com.screamix.adaptiveapiary.registry.ModBlockEntities;
import com.screamix.adaptiveapiary.registry.ModComponents;
import com.screamix.adaptiveapiary.util.InventoryUtil;
import com.screamix.adaptiveapiary.util.RangedItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Пасека. Принцесса и трутень превращаются в королеву; королева каждый рабочий цикл
 * производит продукты и стареет; когда она умирает, появляются принцесса и трутни следующего поколения.
 */
public class ApiaryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_QUEEN = 0;
    public static final int SLOT_DRONE = 1;
    public static final int FRAMES_START = 2;
    public static final int FRAMES_END = 5;
    public static final int OUTPUT_START = 5;
    public static final int OUTPUT_END = 12;
    public static final int SLOTS = 12;

    public static final int DATA_STATUS = 0;
    public static final int DATA_PROGRESS = 1;
    public static final int DATA_LIFE = 2;
    public static final int DATA_COUNT = 3;

    /** Как часто перепроверять цветы, небо и погоду. */
    private static final int ENVIRONMENT_CHECK_INTERVAL = 100;

    private final ItemStackHandler inventory = createInventory(this::setChanged);
    private final IItemHandler automation = new RangedItemHandler(inventory, SLOT_QUEEN, FRAMES_START, OUTPUT_START, OUTPUT_END);
    private final List<ItemStack> pending = new ArrayList<>();

    private ApiaryStatus status = ApiaryStatus.EMPTY;
    /** Вид пчелы в пасеке; клиент знает его, чтобы летающие пчёлы были того же цвета. */
    @Nullable
    private ResourceLocation displaySpecies;
    private ApiaryStatus environment = ApiaryStatus.WORKING;
    private int breedingProgress;
    private int cycleProgress;
    private int environmentTimer;
    @Nullable
    private ResourceLocation environmentSpecies;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_STATUS -> status.ordinal();
                case DATA_PROGRESS -> progressPercent();
                case DATA_LIFE -> lifePercent();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // значения считаются на сервере, клиенту их менять незачем
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ApiaryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.APIARY.get(), pos, state);
    }

    /** Инвентарь пасеки с правилами слотов. Клиентское меню создаёт такой же без привязки к блоку. */
    public static ItemStackHandler createInventory(@Nullable Runnable onChange) {
        return new ItemStackHandler(SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == SLOT_QUEEN) {
                    return BeeItem.isType(stack, BeeType.PRINCESS) || BeeItem.isType(stack, BeeType.QUEEN);
                }
                if (slot == SLOT_DRONE) {
                    return BeeItem.isType(stack, BeeType.DRONE);
                }
                if (slot >= FRAMES_START && slot < FRAMES_END) {
                    return stack.getItem() instanceof FrameItem;
                }
                return true;
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == SLOT_QUEEN || (slot >= FRAMES_START && slot < FRAMES_END) ? 1 : super.getSlotLimit(slot);
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

    // ---- работа ----

    public void serverTick() {
        Level level = this.level;
        if (level == null) {
            return;
        }
        if (!pending.isEmpty()) {
            if (!InventoryUtil.flush(pending, inventory, OUTPUT_START, OUTPUT_END)) {
                status = ApiaryStatus.OUTPUT_FULL;
                updateWorkingState(level);
                updateDisplaySpecies(level);
                return;
            }
            setChanged();
        }

        tickInternal(level);
        updateWorkingState(level);
        updateDisplaySpecies(level);
    }

    private void tickInternal(Level level) {
        ItemStack queenSlot = inventory.getStackInSlot(SLOT_QUEEN);
        BeeType type = BeeItem.typeOf(queenSlot);
        if (type == BeeType.PRINCESS) {
            tickBreeding(queenSlot);
        } else if (type == BeeType.QUEEN) {
            breedingProgress = 0;
            tickQueen(level, queenSlot);
        } else {
            status = ApiaryStatus.EMPTY;
            breedingProgress = 0;
            cycleProgress = 0;
        }
    }

    /** Свойство блока "работает" нужно клиенту для частиц; меняется только при смене состояния. */
    private void updateWorkingState(Level level) {
        boolean working = status == ApiaryStatus.WORKING || status == ApiaryStatus.BREEDING;
        BlockState state = getBlockState();
        if (state.hasProperty(ApiaryBlock.WORKING) && state.getValue(ApiaryBlock.WORKING) != working) {
            level.setBlock(worldPosition, state.setValue(ApiaryBlock.WORKING, working), Block.UPDATE_CLIENTS);
        }
    }

    /** Сообщает клиентам вид пчелы в пасеке, когда он меняется. */
    private void updateDisplaySpecies(Level level) {
        ItemStack bee = inventory.getStackInSlot(SLOT_QUEEN);
        ResourceLocation species = BeeItem.typeOf(bee) == null ? null : BeeItem.genome(bee).activeSpecies().id();
        if (!Objects.equals(species, displaySpecies)) {
            displaySpecies = species;
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** На клиенте: вид пчелы в пасеке или null, если пасека пуста. */
    @Nullable
    public ResourceLocation displaySpecies() {
        return displaySpecies;
    }

    private void tickBreeding(ItemStack princess) {
        cycleProgress = 0;
        ItemStack drone = inventory.getStackInSlot(SLOT_DRONE);
        if (!BeeItem.isType(drone, BeeType.DRONE)) {
            status = ApiaryStatus.NO_DRONE;
            breedingProgress = 0;
            return;
        }
        status = ApiaryStatus.BREEDING;
        if (++breedingProgress < Config.BREEDING_TICKS.get()) {
            return;
        }
        breedingProgress = 0;
        ItemStack queen = BeeItem.createQueen(BeeItem.genome(princess), BeeItem.genome(drone));
        inventory.extractItem(SLOT_DRONE, 1, false);
        inventory.setStackInSlot(SLOT_QUEEN, queen);
        environmentTimer = 0;
        setChanged();
    }

    private void tickQueen(Level level, ItemStack queen) {
        BeeGenome genome = BeeItem.genome(queen);
        BeeSpecies species = genome.activeSpecies();

        // другая королева в слоте: условия надо проверить заново, не дожидаясь таймера
        if (environmentTimer-- <= 0 || !species.id().equals(environmentSpecies)) {
            environment = checkEnvironment(level, species);
            environmentSpecies = species.id();
            environmentTimer = ENVIRONMENT_CHECK_INTERVAL;
        }
        status = environment;
        if (environment != ApiaryStatus.WORKING) {
            return;
        }
        if (++cycleProgress < Config.CYCLE_TICKS.get()) {
            return;
        }
        cycleProgress = 0;
        runCycle(level, queen, genome, species);
        setChanged();
    }

    private ApiaryStatus checkEnvironment(Level level, BeeSpecies species) {
        if (!Config.ENVIRONMENT_CHECKS.get()) {
            return ApiaryStatus.WORKING;
        }
        if (!species.caveDwelling() && level.dimensionType().hasSkyLight()) {
            BlockPos above = worldPosition.above();
            if (!level.canSeeSky(above)) {
                return ApiaryStatus.NO_SKY;
            }
            if (!level.isDay()) {
                return ApiaryStatus.NIGHT;
            }
            if (level.isRainingAt(above)) {
                return ApiaryStatus.RAIN;
            }
        }
        if (!species.flowers().isNear(level, worldPosition)) {
            return ApiaryStatus.NO_FLOWERS;
        }
        return ApiaryStatus.WORKING;
    }

    private void runCycle(Level level, ItemStack queen, BeeGenome genome, BeeSpecies species) {
        RandomSource random = level.getRandom();
        float multiplier = Traits.speedMultiplier(genome.speed()) * (1.0F + FrameItem.BONUS * countFrames());

        produce(species, multiplier, random);
        // Гибрид иногда приносит и продукты второго вида, вдвое реже.
        if (!genome.isPureBred()) {
            produce(genome.secondarySpecies(), multiplier * 0.5F, random);
        }
        wearFrames();

        Integer stored = queen.get(ModComponents.LIFE.get());
        int life = (stored == null ? Traits.lifespanCycles(genome.lifespan()) : stored) - 1;
        if (life > 0) {
            queen.set(ModComponents.LIFE.get(), life);
            return;
        }

        // Королева умерла: следующее поколение.
        BeeGenome mate = queen.get(ModComponents.MATE.get());
        if (mate == null) {
            mate = genome;
        }
        inventory.setStackInSlot(SLOT_QUEEN, ItemStack.EMPTY);
        addOutput(BeeItem.create(BeeType.PRINCESS, BeeBreeding.offspring(genome, mate, level, worldPosition, random)));
        int drones = Traits.droneCount(genome.fertility());
        for (int i = 0; i < drones; i++) {
            addOutput(BeeItem.create(BeeType.DRONE, BeeBreeding.offspring(genome, mate, level, worldPosition, random)));
        }
        environmentTimer = 0;
    }

    private void produce(BeeSpecies species, float multiplier, RandomSource random) {
        for (BeeSpecies.Product product : species.products()) {
            if (random.nextFloat() < product.chance() * multiplier) {
                addOutput(product.stack().get());
            }
        }
    }

    private void addOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack rest = InventoryUtil.insert(inventory, OUTPUT_START, OUTPUT_END, stack);
        if (!rest.isEmpty()) {
            pending.add(rest);
        }
    }

    private int countFrames() {
        int frames = 0;
        for (int slot = FRAMES_START; slot < FRAMES_END; slot++) {
            if (inventory.getStackInSlot(slot).getItem() instanceof FrameItem) {
                frames++;
            }
        }
        return frames;
    }

    private void wearFrames() {
        for (int slot = FRAMES_START; slot < FRAMES_END; slot++) {
            ItemStack frame = inventory.getStackInSlot(slot);
            if (!(frame.getItem() instanceof FrameItem) || !frame.isDamageableItem()) {
                continue;
            }
            int damage = frame.getDamageValue() + 1;
            if (damage >= frame.getMaxDamage()) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            } else {
                frame.setDamageValue(damage);
            }
        }
    }

    // ---- данные для интерфейса ----

    private int progressPercent() {
        if (status == ApiaryStatus.BREEDING) {
            return breedingProgress * 100 / Math.max(1, Config.BREEDING_TICKS.get());
        }
        return cycleProgress * 100 / Math.max(1, Config.CYCLE_TICKS.get());
    }

    private int lifePercent() {
        ItemStack queen = inventory.getStackInSlot(SLOT_QUEEN);
        if (!BeeItem.isType(queen, BeeType.QUEEN)) {
            return 0;
        }
        int max = Traits.lifespanCycles(BeeItem.genome(queen).lifespan());
        Integer life = queen.get(ModComponents.LIFE.get());
        return (life == null ? max : Math.min(life, max)) * 100 / Math.max(1, max);
    }

    // ---- меню ----

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.adaptiveapiary.apiary");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ApiaryMenu(containerId, playerInventory, inventory, data, this);
    }

    // ---- синхронизация с клиентом: только вид пчелы, инвентарь клиенту не нужен ----

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (displaySpecies != null) {
            tag.putString("DisplaySpecies", displaySpecies.toString());
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readDisplaySpecies(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        readDisplaySpecies(packet.getTag());
    }

    private void readDisplaySpecies(CompoundTag tag) {
        displaySpecies = tag.contains("DisplaySpecies") ? ResourceLocation.tryParse(tag.getString("DisplaySpecies")) : null;
    }

    // ---- сохранение ----

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Pending", InventoryUtil.save(pending, registries));
        tag.putInt("BreedingProgress", breedingProgress);
        tag.putInt("CycleProgress", cycleProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        pending.clear();
        pending.addAll(InventoryUtil.load(tag, "Pending", registries));
        breedingProgress = tag.getInt("BreedingProgress");
        cycleProgress = tag.getInt("CycleProgress");
    }
}
