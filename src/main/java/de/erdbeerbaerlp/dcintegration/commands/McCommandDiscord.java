package de.erdbeerbaerlp.dcintegration.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.storage.Configuration;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;


public class McCommandDiscord {
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
                } else
                    ctx.getSource().sendErrorMessage(new StringTextComponent(TextFormatting.RED + "Failed to properly restart the discord bot!"));
            }).start();
            return 0;
        }).then(Commands.literal("ignore")).executes((ctx) -> {
            ctx.getSource().sendFeedback(
                    new StringTextComponent(DiscordIntegration.discord_instance.togglePlayerIgnore(ctx.getSource().asPlayer()) ? Configuration.INSTANCE.msgIgnoreUnignore.get() : Configuration.INSTANCE.msgIgnoreIgnore.get()), true);
            return 0;
        }).then(Commands.literal("link").executes((ctx) -> {
            if (Configuration.INSTANCE.allowLink.get() && !Configuration.INSTANCE.whitelist.get()) {
                final int r = DiscordIntegration.discord_instance.genLinkNumber(ctx.getSource().asPlayer().getUniqueID());
                ctx.getSource().sendFeedback(new StringTextComponent("Send this number as an direct message to the bot to link your account: " + r + "\nThis number will expire after 10 minutes").setStyle(new Style().setColor(TextFormatting.AQUA).setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, r + "")).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to copy number to clipboard")))), true);
            } else {
                ctx.getSource().sendFeedback(new StringTextComponent(TextFormatting.RED + "This subcommand is disabled!"), true);
            }
            return 0;
        }));
        dispatcher.register(l);
    }
}
