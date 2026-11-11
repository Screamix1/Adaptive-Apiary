package com.screamix.adaptiveapiary.registry;

import com.mojang.serialization.Codec;
import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.genetics.BeeGenome;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AdaptiveApiary.MODID);

    /** Геном пчелы (трутень, принцесса, королева). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BeeGenome>> GENOME =
            COMPONENTS.register("genome", () -> DataComponentType.<BeeGenome>builder()
                    .persistent(BeeGenome.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(BeeGenome.CODEC))
                    .cacheEncoding()
                    .build());

    /** Геном трутня, с которым спарилась королева. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BeeGenome>> MATE =
            COMPONENTS.register("mate", () -> DataComponentType.<BeeGenome>builder()
                    .persistent(BeeGenome.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(BeeGenome.CODEC))
                    .cacheEncoding()
                    .build());

    /** Сколько рабочих циклов осталось жить королеве. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LIFE =
            COMPONENTS.register("life", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Материал рудных сот, например "iron" или "tin". */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MATERIAL =
            COMPONENTS.register("material", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    private ModComponents() {}
}
