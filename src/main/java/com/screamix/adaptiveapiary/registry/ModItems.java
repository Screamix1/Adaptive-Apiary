package com.screamix.adaptiveapiary.registry;

import java.util.List;

import com.screamix.adaptiveapiary.AdaptiveApiary;
import com.screamix.adaptiveapiary.block.HiveBlock;
import com.screamix.adaptiveapiary.genetics.BeeGenome;
import com.screamix.adaptiveapiary.genetics.BeeSpecies;
import com.screamix.adaptiveapiary.genetics.Materials;
import com.screamix.adaptiveapiary.genetics.SpeciesRegistry;
import com.screamix.adaptiveapiary.item.BeeBestiaryItem;
import com.screamix.adaptiveapiary.item.BeeItem;
import com.screamix.adaptiveapiary.item.BeeType;
import com.screamix.adaptiveapiary.item.BeealyzerItem;
import com.screamix.adaptiveapiary.item.CombItem;
import com.screamix.adaptiveapiary.item.FrameItem;
import com.screamix.adaptiveapiary.item.ResourceCombItem;
import com.screamix.adaptiveapiary.item.ScoopItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AdaptiveApiary.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AdaptiveApiary.MODID);

    // Пчёлы
    public static final DeferredItem<BeeItem> BEE_DRONE = ITEMS.register("bee_drone",
            () -> new BeeItem(BeeType.DRONE, new Item.Properties()));
    public static final DeferredItem<BeeItem> BEE_PRINCESS = ITEMS.register("bee_princess",
            () -> new BeeItem(BeeType.PRINCESS, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BeeItem> BEE_QUEEN = ITEMS.register("bee_queen",
            () -> new BeeItem(BeeType.QUEEN, new Item.Properties().stacksTo(1)));

    // Соты: первый цвет - мёд в ячейках, второй - восковые стенки (в текстуре они темнее)
    public static final DeferredItem<CombItem> HONEY_COMB = comb("honey_comb", 0xFFC233, 0xC8761A);
    public static final DeferredItem<CombItem> DRIPPING_COMB = comb("dripping_comb", 0xF08A1C, 0xA8480E);
    public static final DeferredItem<CombItem> STRINGY_COMB = comb("stringy_comb", 0xD9C470, 0x8F7A30);
    public static final DeferredItem<CombItem> PARCHED_COMB = comb("parched_comb", 0xF2DF8C, 0xB09040);
    public static final DeferredItem<CombItem> SILKY_COMB = comb("silky_comb", 0xC8E04A, 0x5C8A1E);
    public static final DeferredItem<CombItem> FROZEN_COMB = comb("frozen_comb", 0xE6FAFF, 0x7FC8E0);
    public static final DeferredItem<CombItem> MOSSY_COMB = comb("mossy_comb", 0x8CB04A, 0x3E5A1E);
    public static final DeferredItem<CombItem> ROCKY_COMB = comb("rocky_comb", 0xB8B8B8, 0x6A6A6A);
    public static final DeferredItem<ResourceCombItem> RESOURCE_COMB = ITEMS.register("resource_comb",
            () -> new ResourceCombItem(new Item.Properties()));

    public static final List<DeferredItem<CombItem>> COMBS = List.of(
            HONEY_COMB, DRIPPING_COMB, STRINGY_COMB, PARCHED_COMB, SILKY_COMB, FROZEN_COMB, MOSSY_COMB, ROCKY_COMB);

    // Продукты
    public static final DeferredItem<Item> BEESWAX = ITEMS.registerSimpleItem("beeswax");
    public static final DeferredItem<Item> HONEY_DROP = ITEMS.registerSimpleItem("honey_drop");
    public static final DeferredItem<Item> HONEYDEW = ITEMS.registerSimpleItem("honeydew");
    public static final DeferredItem<Item> PROPOLIS = ITEMS.registerSimpleItem("propolis");

    // Инструменты
    public static final DeferredItem<ScoopItem> SCOOP = ITEMS.register("scoop",
            () -> new ScoopItem(new Item.Properties().durability(48)));
    public static final DeferredItem<FrameItem> FRAME = ITEMS.register("frame",
            () -> new FrameItem(new Item.Properties().durability(80)));
    public static final DeferredItem<BeealyzerItem> BEEALYZER = ITEMS.register("beealyzer",
            () -> new BeealyzerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BeeBestiaryItem> BEE_BESTIARY = ITEMS.register("bee_bestiary",
            () -> new BeeBestiaryItem(new Item.Properties().stacksTo(1)));

    // Блоки
    public static final DeferredItem<BlockItem> APIARY = ITEMS.registerSimpleBlockItem(ModBlocks.APIARY);
    public static final DeferredItem<BlockItem> CENTRIFUGE = ITEMS.registerSimpleBlockItem(ModBlocks.CENTRIFUGE);

    static {
        for (DeferredBlock<HiveBlock> hive : ModBlocks.HIVES) {
            ITEMS.registerSimpleBlockItem(hive);
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.adaptiveapiary"))
                    .icon(() -> BeeItem.create(BeeType.QUEEN, BeeGenome.pure(SpeciesRegistry.get(SpeciesRegistry.IMPERIAL))))
                    .displayItems((parameters, output) -> {
                        output.accept(APIARY.get());
                        output.accept(CENTRIFUGE.get());
                        output.accept(SCOOP.get());
                        output.accept(FRAME.get());
                        output.accept(BEEALYZER.get());
                        output.accept(BEE_BESTIARY.get());
                        for (DeferredBlock<HiveBlock> hive : ModBlocks.HIVES) {
                            output.accept(hive.get());
                        }
                        output.accept(BEESWAX.get());
                        output.accept(HONEY_DROP.get());
                        output.accept(HONEYDEW.get());
                        output.accept(PROPOLIS.get());
                        for (DeferredItem<CombItem> comb : COMBS) {
                            output.accept(comb.get());
                        }
                        for (String material : Materials.all().keySet()) {
                            output.accept(ResourceCombItem.create(material));
                        }
                        // чистокровные пчёлы всех видов, включая рудных для установленных модов
                        for (BeeSpecies species : SpeciesRegistry.allKnown()) {
                            BeeGenome genome = BeeGenome.pure(species).withAnalyzed(true);
                            output.accept(BeeItem.create(BeeType.PRINCESS, genome));
                            output.accept(BeeItem.create(BeeType.DRONE, genome));
                        }
                    })
                    .build());

    private static DeferredItem<CombItem> comb(String name, int primary, int secondary) {
        return ITEMS.register(name, () -> new CombItem(primary, secondary, new Item.Properties()));
    }

    private ModItems() {}
}
