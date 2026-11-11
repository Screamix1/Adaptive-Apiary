package com.screamix.adaptiveapiary.blockentity;

import net.minecraft.network.chat.Component;

/** Что сейчас происходит в пасеке. Порядок важен: номер передаётся клиенту. */
public enum ApiaryStatus {
    EMPTY("empty"),
    NO_DRONE("no_drone"),
    BREEDING("breeding"),
    WORKING("working"),
    NO_FLOWERS("no_flowers"),
    NIGHT("night"),
    NO_SKY("no_sky"),
    RAIN("rain"),
    OUTPUT_FULL("output_full");

    private static final ApiaryStatus[] VALUES = values();

    private final String key;

    ApiaryStatus(String key) {
        this.key = key;
    }

    public Component message() {
        return Component.translatable("gui.adaptiveapiary.status." + key);
    }

    public boolean isProblem() {
        return ordinal() >= NO_FLOWERS.ordinal() || this == NO_DRONE;
    }

    public static ApiaryStatus byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : EMPTY;
    }
}
