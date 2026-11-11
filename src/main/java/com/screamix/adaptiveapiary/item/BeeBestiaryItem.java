package com.screamix.adaptiveapiary.item;

import java.util.List;
import java.util.Set;

import com.screamix.adaptiveapiary.genetics.BeeDiscoveries;
import com.screamix.adaptiveapiary.menu.BeeBestiaryMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class BeeBestiaryItem extends Item {
    public BeeBestiaryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            Set<ResourceLocation> discoveries = BeeDiscoveries.collect(serverPlayer);
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, owner) -> new BeeBestiaryMenu(id, discoveries), getName(held)),
                    buffer -> buffer.writeCollection(discoveries, (buf, species) -> buf.writeResourceLocation(species)));
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.adaptiveapiary.bee_bestiary").withStyle(ChatFormatting.GRAY));
    }
}
