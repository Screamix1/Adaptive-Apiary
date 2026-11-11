package com.screamix.adaptiveapiary.genetics;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Что пчёлам нужно рядом с пасекой, чтобы работать. */
public enum FlowerType {
    FLOWERS("flowers", s -> s.is(BlockTags.FLOWERS)),
    DESERT("desert", s -> s.is(Blocks.CACTUS) || s.is(Blocks.DEAD_BUSH) || s.is(BlockTags.FLOWERS)),
    JUNGLE("jungle", s -> s.is(Blocks.VINE) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN)
            || s.is(Blocks.COCOA) || s.is(BlockTags.FLOWERS)),
    ROCK("rock", s -> s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.BASE_STONE_NETHER)
            || s.is(Blocks.COBBLESTONE) || s.is(Blocks.END_STONE));

    private static final int RADIUS = 4;
    private static final int HEIGHT = 2;

    private final String key;
    private final Predicate<BlockState> matcher;

    FlowerType(String key, Predicate<BlockState> matcher) {
        this.key = key;
        this.matcher = matcher;
    }

    public Component displayName() {
        return Component.translatable("flowers.adaptiveapiary." + key);
    }

    /** Есть ли подходящий блок в области 9x5x9 вокруг пасеки. */
    public boolean isNear(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -HEIGHT, -RADIUS),
                center.offset(RADIUS, HEIGHT, RADIUS))) {
            if (level.isLoaded(pos) && matcher.test(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }
}
