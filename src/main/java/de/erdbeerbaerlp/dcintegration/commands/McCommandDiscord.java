package de.erdbeerbaerlp.dcintegration.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.erdbeerbaerlp.dcintegration.Configuration;
import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;


public class McCommandDiscord
{
    public McCommandDiscord(CommandDispatcher<CommandSource> dispatcher) {
        final LiteralArgumentBuilder<CommandSource> l = LiteralArgumentBuilder.<CommandSource>literal("discord").executes((ctx) -> {
            ctx.getSource().sendFeedback(new StringTextComponent(Configuration.INSTANCE.dcCmdMsg.get()).setStyle(
                    new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(Configuration.INSTANCE.dcCmdMsgHover.get())))
                               .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Configuration.INSTANCE.dcCmdURL.get()))), false);
            return 0;
        }).then(Commands.literal("restart").requires((p) -> p.hasPermissionLevel(3))).executes((ctx) -> {
            new Thread(() -> {
                if (DiscordIntegration.discord_instance.restart()) {
                    ctx.getSource().sendFeedback(new StringTextComponent(TextFormatting.GREEN + "Discord bot restarted!"), false);
                }
                else ctx.getSource().sendErrorMessage(new StringTextComponent(TextFormatting.RED + "Failed to properly restart the discord bot!"));
            }).start();
            return 0;
        }).then(Commands.literal("ignore")).executes((ctx) -> {
            ctx.getSource().sendFeedback(
                    new StringTextComponent(DiscordIntegration.discord_instance.togglePlayerIgnore(ctx.getSource().asPlayer()) ? Configuration.INSTANCE.msgIgnoreUnignore.get() : Configuration.INSTANCE.msgIgnoreIgnore.get()), true);
            return 0;
        });
        dispatcher.register(l);
    }
}
