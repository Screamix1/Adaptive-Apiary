package com.screamix.adaptiveapiary.particle;

import com.mojang.serialization.MapCodec;
import com.screamix.adaptiveapiary.registry.ModParticles;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** Летающая пчела вида {@code species}: клиент красит её так же, как пчелу этого вида в инвентаре. */
public record BeeParticleOptions(ResourceLocation species) implements ParticleOptions {
    public static final MapCodec<BeeParticleOptions> CODEC =
            ResourceLocation.CODEC.fieldOf("species").xmap(BeeParticleOptions::new, BeeParticleOptions::species);
    public static final StreamCodec<ByteBuf, BeeParticleOptions> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(BeeParticleOptions::new, BeeParticleOptions::species);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.BEE.get();
    }
}
