package com.screamix.adaptiveapiary.item;

import net.minecraft.world.item.Item;

/** Соты обычных видов. Клиент красит общую текстуру: первый цвет - мёд, второй - восковые стенки. */
public class CombItem extends Item {
    private final int primaryColor;
    private final int secondaryColor;

    public CombItem(int primaryColor, int secondaryColor, Properties properties) {
        super(properties);
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    public int primaryColor() {
        return primaryColor;
    }

    public int secondaryColor() {
        return secondaryColor;
    }
}
