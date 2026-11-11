package com.screamix.adaptiveapiary.client;

import com.screamix.adaptiveapiary.genetics.BeeSpecies;

/**
 * Цвета пчелы вида: одни и те же для предмета в инвентаре и для пчёл, летающих у улья.
 * Контур - основной цвет вида, тело - второй. У рудных пчёл тело в цвет материала, контур темнее.
 */
public final class BeeColors {
    public static int outline(BeeSpecies species) {
        if (species.isOreSpecies()) {
            return MaterialColors.darken(MaterialColors.get(species.material()), 0.6F);
        }
        return species.primaryColor() & 0xFFFFFF;
    }

    public static int body(BeeSpecies species) {
        if (species.isOreSpecies()) {
            return MaterialColors.get(species.material());
        }
        return species.secondaryColor() & 0xFFFFFF;
    }

    private BeeColors() {}
}
