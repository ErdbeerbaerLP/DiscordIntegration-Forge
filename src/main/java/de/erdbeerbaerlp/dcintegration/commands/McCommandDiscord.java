package de.erdbeerbaerlp.dcintegration.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.storage.PlayerLinkController;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;


public class McCommandDiscord {
    public McCommandDiscord(CommandDispatcher<CommandSource> dispatcher) {
        final LiteralArgumentBuilder<CommandSource> l = Commands.literal("discord");
        if (Configuration.INSTANCE.dcCmdEnabled.get()) l.executes((ctx) -> {
            ctx.getSource().sendFeedback(TextComponentUtils.func_240648_a_(new StringTextComponent(Configuration.INSTANCE.dcCmdMsg.get()),
                    Style.field_240709_b_.func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent(Configuration.INSTANCE.dcCmdMsgHover.get())))
                            .func_240715_a_(new ClickEvent(ClickEvent.Action.OPEN_URL, Configuration.INSTANCE.dcCmdURL.get()))), false);
            return 0;
        });
        l.then(Commands.literal("restart").requires((p) -> p.hasPermissionLevel(3)).executes((ctx) -> {
            new Thread(() -> {
                if (DiscordIntegration.discord_instance.restart()) {
                    ctx.getSource().sendFeedback(new StringTextComponent(TextFormatting.GREEN + "Discord bot restarted!"), true);
                } else
                    ctx.getSource().sendErrorMessage(new StringTextComponent(TextFormatting.RED + "Failed to properly restart the discord bot!"));
            }).start();
            return 0;
        })).then(Commands.literal("ignore").executes((ctx) -> {
            ctx.getSource().sendFeedback(
                    new StringTextComponent(DiscordIntegration.discord_instance.togglePlayerIgnore(ctx.getSource().asPlayer()) ? Configuration.INSTANCE.msgIgnoreUnignore.get() : Configuration.INSTANCE.msgIgnoreIgnore.get()), true);
            return 0;
        })).then(Commands.literal("link").executes((ctx) -> {
            if (Configuration.INSTANCE.allowLink.get() && ServerLifecycleHooks.getCurrentServer().isServerInOnlineMode() && !Configuration.INSTANCE.whitelist.get()) {
                if (PlayerLinkController.isPlayerLinked(ctx.getSource().asPlayer().getUniqueID())) {
                    ctx.getSource().sendFeedback(new StringTextComponent(TextFormatting.RED + "You are already linked with " + PlayerLinkController.getDiscordFromPlayer(ctx.getSource().asPlayer().getUniqueID())), false);
                    return 0;
                }
                final int r = DiscordIntegration.discord_instance.genLinkNumber(ctx.getSource().asPlayer().getUniqueID());
                ctx.getSource().sendFeedback(TextComponentUtils.func_240648_a_(new StringTextComponent("Send this number as an direct message to the bot to link your account: " + r + "\nThis number will expire after 10 minutes"), Style.field_240709_b_.func_240721_b_(TextFormatting.AQUA).func_240715_a_(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, r + "")).func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("Click to copy number to clipboard")))), false);
            } else {
                ctx.getSource().sendFeedback(new StringTextComponent(TextFormatting.RED + "This subcommand is disabled!"), false);
            }
            return 0;
        }));
        dispatcher.register(l);
    }
}
