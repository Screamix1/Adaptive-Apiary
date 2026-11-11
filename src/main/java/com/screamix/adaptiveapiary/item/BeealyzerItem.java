package com.screamix.adaptiveapiary.item;

import java.util.List;

import com.screamix.adaptiveapiary.genetics.BeeDiscoveries;
import com.screamix.adaptiveapiary.genetics.BeeGenome;
import com.screamix.adaptiveapiary.registry.ModComponents;
import com.screamix.adaptiveapiary.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Пчелоанализатор. По ПКМ исследует всех пчёл в инвентаре игрока,
 * тратя одну каплю мёда на каждый стек. После этого в подсказке виден весь геном.
 */
public class BeealyzerItem extends Item {
    public BeealyzerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(held);
        }
        Inventory inventory = player.getInventory();
        int analyzed = 0;
        boolean outOfHoney = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (BeeItem.typeOf(stack) == null) {
                continue;
            }
            BeeGenome genome = BeeItem.genome(stack);
            if (genome.analyzed()) {
                continue;
            }
            if (!player.getAbilities().instabuild && !consumeHoneyDrop(inventory)) {
                outOfHoney = true;
                break;
            }
            stack.set(ModComponents.GENOME.get(), genome.withAnalyzed(true));
            analyzed++;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BeeDiscoveries.collect(serverPlayer);
        }
        if (analyzed > 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.4F);
            player.displayClientMessage(Component.translatable("message.adaptiveapiary.analyzed", analyzed), true);
        } else if (outOfHoney) {
            player.displayClientMessage(Component.translatable("message.adaptiveapiary.no_honey"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.adaptiveapiary.nothing_to_analyze"), true);
        }
        return InteractionResultHolder.consume(held);
    }

    private static boolean consumeHoneyDrop(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.HONEY_DROP.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.adaptiveapiary.beealyzer").withStyle(ChatFormatting.GRAY));
    }
}
