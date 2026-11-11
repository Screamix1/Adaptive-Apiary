package com.screamix.adaptiveapiary.registry;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.blockentity.ApiaryBlockEntity;
import com.screamix.adaptiveapiary.blockentity.CentrifugeBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AdaptiveApiary.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ApiaryBlockEntity>> APIARY =
            BLOCK_ENTITIES.register("apiary",
                    () -> BlockEntityType.Builder.of(ApiaryBlockEntity::new, ModBlocks.APIARY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE =
            BLOCK_ENTITIES.register("centrifuge",
                    () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, ModBlocks.CENTRIFUGE.get()).build(null));

    private ModBlockEntities() {}
}
