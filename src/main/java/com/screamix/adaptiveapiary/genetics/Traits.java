package com.screamix.adaptiveapiary.genetics;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Значения признаков по индексам. Числа взяты такими же, как в Forestry. */
public final class Traits {
    private static final float[] SPEED = {0.3F, 0.6F, 0.8F, 1.0F, 1.2F, 1.4F, 1.7F};
    private static final int[] LIFESPAN = {10, 20, 30, 35, 40, 45, 50, 60, 70};

    public static final int SPEED_SLOWEST = 0;
    public static final int SPEED_SLOWER = 1;
    public static final int SPEED_SLOW = 2;
    public static final int SPEED_NORMAL = 3;
    public static final int SPEED_FAST = 4;
    public static final int SPEED_FASTER = 5;
    public static final int SPEED_FASTEST = 6;

    public static final int LIFE_SHORTEST = 0;
    public static final int LIFE_SHORTER = 1;
    public static final int LIFE_SHORT = 2;
    public static final int LIFE_SHORTENED = 3;
    public static final int LIFE_NORMAL = 4;
    public static final int LIFE_ELONGATED = 5;
    public static final int LIFE_LONG = 6;
    public static final int LIFE_LONGER = 7;
    public static final int LIFE_LONGEST = 8;

    /** Множитель шанса производства. */
    public static float speedMultiplier(int index) {
        return SPEED[Mth.clamp(index, 0, SPEED.length - 1)];
    }

    /** Срок жизни королевы в рабочих циклах. */
    public static int lifespanCycles(int index) {
        return LIFESPAN[Mth.clamp(index, 0, LIFESPAN.length - 1)];
    }

    /** Число трутней в потомстве. */
    public static int droneCount(int fertility) {
        return Mth.clamp(fertility, 1, 4);
    }

    public static Component speedName(int index) {
        return Component.translatable("trait.adaptiveapiary.speed." + Mth.clamp(index, 0, SPEED.length - 1));
    }

    public static Component lifespanName(int index) {
        return Component.translatable("trait.adaptiveapiary.lifespan." + Mth.clamp(index, 0, LIFESPAN.length - 1));
    }

    private Traits() {}
}
