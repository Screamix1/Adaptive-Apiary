package com.screamix.adaptiveapiary.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.screamix.adaptiveapiary.blockentity.ApiaryBlockEntity;
import com.screamix.adaptiveapiary.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ApiaryBlock extends BaseEntityBlock {
    public static final MapCodec<ApiaryBlock> CODEC = simpleCodec(ApiaryBlock::new);

    /** Пасека работает или спаривает пчёл: клиент по этому свойству рисует летающих пчёл. */
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    public ApiaryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORKING);
    }

    /** Вокруг работающей пасеки летают и жужжат пчёлы того вида, что сидит внутри. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(WORKING) && level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary
                && apiary.displaySpecies() != null) {
            HiveBlock.spawnBees(level, pos, random, 1 + random.nextInt(2), apiary.displaySpecies());
            HiveBlock.buzz(level, pos, random);
        }
    }

    @Override
    public MapCodec<ApiaryBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ApiaryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.APIARY.get(), (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary) {
            serverPlayer.openMenu(apiary, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary) {
            apiary.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
