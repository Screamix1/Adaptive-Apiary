package com.screamix.adaptiveapiary.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.screamix.adaptiveapiary.genetics.BeeGenome;
import com.screamix.adaptiveapiary.genetics.BeeSpecies;
import com.screamix.adaptiveapiary.genetics.SpeciesRegistry;
import com.screamix.adaptiveapiary.item.BeeItem;
import com.screamix.adaptiveapiary.item.BeeType;
import com.screamix.adaptiveapiary.item.ScoopItem;
import com.screamix.adaptiveapiary.particle.BeeParticleOptions;
import com.screamix.adaptiveapiary.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Дикий улей. Если сломать его сачком, выпадают принцесса, трутни и иногда соты. */
public class HiveBlock extends Block {
    private final Supplier<ResourceLocation> species;

    public HiveBlock(Supplier<ResourceLocation> species, Properties properties) {
        super(properties);
        this.species = species;
    }

    /** Вокруг дикого улья кружат и жужжат пчёлы: днём и без дождя, а пещерные виды - всегда. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BeeSpecies beeSpecies = SpeciesRegistry.get(species.get());
        if (!beeSpecies.caveDwelling() && (!level.isDay() || level.isRainingAt(pos.above()))) {
            return;
        }
        spawnBees(level, pos, random, 1 + random.nextInt(2), beeSpecies.id());
        buzz(level, pos, random);
    }

    /**
     * Гул пчёл. Игра вызывает animateTick для блоков рядом с игроком чаще, чем для дальних,
     * поэтому вблизи записи (по 5,5 секунды, с плавным началом и концом) идут почти без перерыва,
     * а издалека слышны реже и тише. Слышно в пределах 10 блоков.
     */
    public static void buzz(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.HIVE_BUZZ.get(),
                    SoundSource.BLOCKS, 0.45F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }

    /** Пчёлы вида {@code species} вокруг блока; раскрашены так же, как пчела этого вида в инвентаре. */
    public static void spawnBees(Level level, BlockPos pos, RandomSource random, int count, ResourceLocation species) {
        BeeParticleOptions bee = new BeeParticleOptions(species);
        for (int i = 0; i < count; i++) {
            level.addParticle(bee, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null || !(tool.getItem() instanceof ScoopItem)) {
            return List.of();
        }
        RandomSource random = params.getLevel().getRandom();
        BeeSpecies beeSpecies = SpeciesRegistry.get(species.get());
        BeeGenome genome = BeeGenome.pure(beeSpecies);

        List<ItemStack> drops = new ArrayList<>();
        drops.add(BeeItem.create(BeeType.PRINCESS, genome));
        ItemStack drones = BeeItem.create(BeeType.DRONE, genome);
        drones.setCount(1 + random.nextInt(3));
        drops.add(drones);
        if (!beeSpecies.products().isEmpty() && random.nextFloat() < 0.5F) {
            drops.add(beeSpecies.products().get(0).stack().get());
        }
        return drops;
    }
}
