package com.screamix.adaptiveapiary.registry;

import com.screamix.adaptiveapiary.AdaptiveApiary;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, AdaptiveApiary.MODID);

    /** Гул пчёл у дикого улья и у работающей пасеки (три записи, см. sounds.json). */
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_BUZZ =
            SOUNDS.register("block.hive.buzz", () -> SoundEvent.createVariableRangeEvent(AdaptiveApiary.id("block.hive.buzz")));

    private ModSounds() {}
}
