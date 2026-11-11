package com.screamix.adaptiveapiary.command;

import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.screamix.adaptiveapiary.genetics.Materials;
import com.screamix.adaptiveapiary.genetics.Materials.MaterialInfo;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /adaptiveapiary materials [фильтр] - какие руды мод нашёл, что дают их соты и откуда это взято.
 * Наведите курсор на строку, чтобы увидеть, по каким тегам и именам найдена руда.
 */
public final class ApiaryCommand {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("adaptiveapiary")
                .then(Commands.literal("materials")
                        .executes(ctx -> list(ctx.getSource(), ""))
                        .then(Commands.argument("filter", StringArgumentType.word())
                                .executes(ctx -> list(ctx.getSource(), StringArgumentType.getString(ctx, "filter"))))));
    }

    private static int list(CommandSourceStack source, String filter) {
        // кэш сам сбрасывается при /reload, смене конфига и запуске сервера
        Map<String, MaterialInfo> all = Materials.all();
        source.sendSuccess(() -> Component.translatable("command.adaptiveapiary.materials.header", all.size())
                .withStyle(ChatFormatting.YELLOW), false);
        int shown = 0;
        for (MaterialInfo info : all.values()) {
            if (!filter.isEmpty() && !info.name().contains(filter)) {
                continue;
            }
            StringBuilder hover = new StringBuilder();
            hover.append("ores: ");
            info.ores().forEach(ore -> hover.append(BuiltInRegistries.ITEM.getKey(ore)).append(' '));
            for (String found : info.sources()) {
                hover.append('\n').append(found);
            }
            hover.append("\nproduct: ").append(info.productSource());
            MutableComponent line = Component.literal(" - ")
                    .append(Component.literal(info.name()).withStyle(ChatFormatting.GOLD))
                    .append(" -> ")
                    .append(new ItemStack(info.product()).getHoverName())
                    .append(Component.literal(" (" + BuiltInRegistries.ITEM.getKey(info.product()) + ")").withStyle(ChatFormatting.GRAY));
            line.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover.toString()))));
            source.sendSuccess(() -> line, false);
            shown++;
        }
        return shown;
    }

    private ApiaryCommand() {}
}
