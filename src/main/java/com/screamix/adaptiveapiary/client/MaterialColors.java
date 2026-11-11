package com.screamix.adaptiveapiary.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.screamix.adaptiveapiary.genetics.Materials;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

/**
 * Цвет материала для рудных пчёл и сот. Для ванильных материалов цвета заданы вручную,
 * для материалов из модов берётся средний цвет текстуры предмета, так что пчела
 * "железа" серая, а пчела неизвестной руды из любого мода получает цвет этой руды.
 */
public final class MaterialColors {
    private static final Map<String, Integer> KNOWN = Map.ofEntries(
            Map.entry("coal", 0x3B3B3B),
            Map.entry("iron", 0xD8AF93),
            Map.entry("copper", 0xE77C56),
            Map.entry("gold", 0xFDF55F),
            Map.entry("redstone", 0xE01A0E),
            Map.entry("lapis", 0x345EC3),
            Map.entry("diamond", 0x4AEDD9),
            Map.entry("emerald", 0x17DD62),
            Map.entry("quartz", 0xE8DDD0),
            Map.entry("netherite_scrap", 0x6B4A43));

    private static final Map<String, Integer> CACHE = new ConcurrentHashMap<>();

    /** Цвет материала в формате RGB. */
    public static int get(String material) {
        Integer cached = CACHE.get(material);
        if (cached != null) {
            return cached;
        }
        int color = compute(material);
        CACHE.put(material, color);
        return color;
    }

    public static void clear() {
        CACHE.clear();
    }

    public static int darken(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    private static int compute(String material) {
        Integer known = KNOWN.get(material);
        if (known != null) {
            return known;
        }
        try {
            ItemStack product = Materials.product(material);
            if (!product.isEmpty()) {
                int sampled = averageColor(product);
                if (sampled >= 0) {
                    return sampled;
                }
            }
        } catch (RuntimeException e) {
            // текстуры могут быть недоступны (например, во время перезагрузки ресурсов): берём запасной цвет
        }
        return hashColor(material);
    }

    /**
     * Средний цвет непрозрачных пикселей текстуры предмета или -1, если посчитать не удалось.
     * Яркие цветные пиксели весят больше серых: у руды без своего продукта цвет берётся
     * от вкраплений, а не от камня вокруг них.
     */
    private static int averageColor(ItemStack stack) {
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        TextureAtlasSprite sprite = model.getParticleIcon();
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        double r = 0;
        double g = 0;
        double b = 0;
        double total = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // формат пикселя: ABGR
                int pixel = sprite.getPixelRGBA(0, x, y);
                if ((pixel >>> 24) < 128) {
                    continue;
                }
                int pr = pixel & 0xFF;
                int pg = (pixel >> 8) & 0xFF;
                int pb = (pixel >> 16) & 0xFF;
                double chroma = (Math.max(pr, Math.max(pg, pb)) - Math.min(pr, Math.min(pg, pb))) / 255.0;
                double weight = (0.12 + chroma) * (0.12 + chroma);
                r += pr * weight;
                g += pg * weight;
                b += pb * weight;
                total += weight;
            }
        }
        if (total <= 0) {
            return -1;
        }
        return ((int) (r / total) << 16) | ((int) (g / total) << 8) | (int) (b / total);
    }

    /** Запасной цвет: стабильный и достаточно яркий, зависит только от имени материала. */
    private static int hashColor(String material) {
        int hash = material.hashCode();
        int r = 96 + (hash & 0x7F);
        int g = 96 + ((hash >> 7) & 0x7F);
        int b = 96 + ((hash >> 14) & 0x7F);
        return (r << 16) | (g << 8) | b;
    }

    private MaterialColors() {}
}
