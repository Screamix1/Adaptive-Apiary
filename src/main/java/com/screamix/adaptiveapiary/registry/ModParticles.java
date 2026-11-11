package com.screamix.adaptiveapiary.registry;

import com.mojang.serialization.MapCodec;
import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.particle.BeeParticleOptions;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, AdaptiveApiary.MODID);

    /** Пчела, которая кружит вокруг улья или пасеки. Координаты частицы - центр блока, в параметре - вид пчелы. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<BeeParticleOptions>> BEE =
            PARTICLES.register("bee", () -> new ParticleType<BeeParticleOptions>(false) {
                @Override
                public MapCodec<BeeParticleOptions> codec() {
                    return BeeParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, BeeParticleOptions> streamCodec() {
                    return BeeParticleOptions.STREAM_CODEC;
                }
            });

    private ModParticles() {}
}
