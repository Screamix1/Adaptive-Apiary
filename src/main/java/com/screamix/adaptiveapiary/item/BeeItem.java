package com.screamix.adaptiveapiary.item;

import java.util.List;

import javax.annotation.Nullable;

import com.screamix.adaptiveapiary.genetics.BeeGenome;
import com.screamix.adaptiveapiary.genetics.BeeSpecies;
import com.screamix.adaptiveapiary.genetics.SpeciesRegistry;
import com.screamix.adaptiveapiary.genetics.Traits;
import com.screamix.adaptiveapiary.registry.ModComponents;
import com.screamix.adaptiveapiary.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Пчела-предмет. Один класс на трутня, принцессу и королеву; геном лежит в компоненте данных. */
public class BeeItem extends Item {
    private final BeeType type;

    public BeeItem(BeeType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public BeeType type() {
        return type;
    }

    /** Тип пчелы в стеке или null, если это не пчела. */
    @Nullable
    public static BeeType typeOf(ItemStack stack) {
        return stack.getItem() instanceof BeeItem bee ? bee.type : null;
    }

    public static boolean isType(ItemStack stack, BeeType type) {
        return typeOf(stack) == type;
    }

    /** Геном пчелы. Если компонента нет (предмет выдан командой), считаем пчелу чистой лесной. */
    public static BeeGenome genome(ItemStack stack) {
        BeeGenome genome = stack.get(ModComponents.GENOME.get());
        return genome != null ? genome : BeeGenome.pure(SpeciesRegistry.get(SpeciesRegistry.FOREST));
    }

    public static ItemStack create(BeeType type, BeeGenome genome) {
        Item item = switch (type) {
            case DRONE -> ModItems.BEE_DRONE.get();
            case PRINCESS -> ModItems.BEE_PRINCESS.get();
            case QUEEN -> ModItems.BEE_QUEEN.get();
        };
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.GENOME.get(), genome);
        return stack;
    }

    /** Королева: геном принцессы, геном трутня и полный срок жизни. */
    public static ItemStack createQueen(BeeGenome princess, BeeGenome drone) {
        ItemStack stack = create(BeeType.QUEEN, princess);
        stack.set(ModComponents.MATE.get(), drone);
        stack.set(ModComponents.LIFE.get(), Traits.lifespanCycles(princess.lifespan()));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack), genome(stack).activeSpecies().displayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BeeGenome genome = genome(stack);
        if (type == BeeType.QUEEN) {
            Integer life = stack.get(ModComponents.LIFE.get());
            int max = Traits.lifespanCycles(genome.lifespan());
            tooltip.add(Component.translatable("tooltip.adaptiveapiary.life", life == null ? max : life, max)
                    .withStyle(ChatFormatting.GRAY));
        }
        if (genome.species().equals(SpeciesRegistry.MINERAL)) {
            tooltip.add(Component.translatable("tooltip.adaptiveapiary.mineral_hint").withStyle(ChatFormatting.GOLD));
        }
        if (!genome.analyzed()) {
            tooltip.add(Component.translatable("tooltip.adaptiveapiary.unknown_genome")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        BeeSpecies active = genome.activeSpecies();
        BeeSpecies inactive = genome.secondarySpecies();
        if (genome.isPureBred()) {
            tooltip.add(Component.translatable("tooltip.adaptiveapiary.pure_bred", active.displayName())
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.adaptiveapiary.hybrid", active.displayName(), inactive.displayName())
                    .withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.adaptiveapiary.speed",
                Traits.speedName(genome.speed()), Traits.speedName(genome.inactiveSpeed())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.adaptiveapiary.lifespan",
                Traits.lifespanName(genome.lifespan()), Traits.lifespanName(genome.inactiveLifespan())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.adaptiveapiary.fertility",
                genome.fertility(), genome.inactiveFertility()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.adaptiveapiary.flowers", active.flowers().displayName())
                .withStyle(ChatFormatting.GRAY));
        if (active.caveDwelling()) {
            tooltip.add(Component.translatable("tooltip.adaptiveapiary.cave_dwelling").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
