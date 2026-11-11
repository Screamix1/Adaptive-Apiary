package com.screamix.adaptiveapiary.registry;

import java.util.List;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.block.ApiaryBlock;
import com.screamix.adaptiveapiary.block.CentrifugeBlock;
import com.screamix.adaptiveapiary.block.HiveBlock;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AdaptiveApiary.MODID);

    public static final DeferredBlock<HiveBlock> FOREST_HIVE = hive("forest_hive", MapColor.COLOR_YELLOW);
    public static final DeferredBlock<HiveBlock> MEADOWS_HIVE = hive("meadows_hive", MapColor.COLOR_RED);
    public static final DeferredBlock<HiveBlock> MODEST_HIVE = hive("modest_hive", MapColor.SAND);
    public static final DeferredBlock<HiveBlock> TROPICAL_HIVE = hive("tropical_hive", MapColor.COLOR_GREEN);
    public static final DeferredBlock<HiveBlock> WINTRY_HIVE = hive("wintry_hive", MapColor.SNOW);
    public static final DeferredBlock<HiveBlock> MARSHY_HIVE = hive("marshy_hive", MapColor.COLOR_BROWN);
    public static final DeferredBlock<HiveBlock> ROCKY_HIVE = hive("rocky_hive", MapColor.STONE);

    public static final List<DeferredBlock<HiveBlock>> HIVES = List.of(
            FOREST_HIVE, MEADOWS_HIVE, MODEST_HIVE, TROPICAL_HIVE, WINTRY_HIVE, MARSHY_HIVE, ROCKY_HIVE);

    public static final DeferredBlock<ApiaryBlock> APIARY = BLOCKS.register("apiary",
            () -> new ApiaryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD)));

    public static final DeferredBlock<CentrifugeBlock> CENTRIFUGE = BLOCKS.register("centrifuge",
            () -> new CentrifugeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));

    /** Имя улья "forest_hive" соответствует виду "forest" из SpeciesRegistry. */
    private static DeferredBlock<HiveBlock> hive(String name, MapColor color) {
        String speciesPath = name.substring(0, name.length() - "_hive".length());
        return BLOCKS.register(name, () -> new HiveBlock(
                () -> ResourceLocation.fromNamespaceAndPath(AdaptiveApiary.MODID, speciesPath),
                BlockBehaviour.Properties.of().mapColor(color).strength(1.0F).sound(SoundType.WOOD).noLootTable()));
    }

    private ModBlocks() {}
}
