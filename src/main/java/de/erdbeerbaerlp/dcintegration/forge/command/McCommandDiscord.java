package de.erdbeerbaerlp.dcintegration.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.erdbeerbaerlp.dcintegration.common.addon.AddonLoader;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.io.IOException;


public class McCommandDiscord {
    public McCommandDiscord(CommandDispatcher<CommandSource> dispatcher) {
        final LiteralArgumentBuilder<CommandSource> l = Commands.literal("discord");
        if (Configuration.instance().ingameCommand.enabled) l.executes((ctx) -> {
            ctx.getSource().sendFeedback(TextComponentUtils.func_240648_a_(new StringTextComponent(Configuration.instance().ingameCommand.message),
                    Style.EMPTY.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(Configuration.instance().ingameCommand.hoverMessage)))
                            .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Configuration.instance().ingameCommand.inviteURL))), false);
            return 0;
        });
        l.then(Commands.literal("restart").requires((p) -> p.hasPermissionLevel(3)).executes((ctx) -> {
            new Thread(() -> {
                if (Variables.discord_instance.restart()) {
                    ctx.getSource().sendFeedback(new StringTextComponent(TextFormatting.GREEN + "Discord bot restarted!"), true);
                } else
                    ctx.getSource().sendErrorMessage(new StringTextComponent(TextFormatting.RED + "Failed to properly restart the discord bot!"));
            }).start();
            return 0;
        })).then(Commands.literal("ignore").executes((ctx) -> {
            ctx.getSource().sendFeedback(
                    new StringTextComponent(Variables.discord_instance.togglePlayerIgnore(ctx.getSource().asPlayer().getUniqueID()) ? Configuration.instance().localization.commands.commandIgnore_unignore : Configuration.instance().localization.commands.commandIgnore_ignore), true);
            return 0;
        })).then(Commands.literal("link").executes((ctx) -> {
            if (Configuration.instance().linking.enableLinking && ServerLifecycleHooks.getCurrentServer().isServerInOnlineMode() && !Configuration.instance().linking.whitelistMode) {
                if (PlayerLinkController.isPlayerLinked(ctx.getSource().asPlayer().getUniqueID())) {
                    ctx.getSource().sendFeedback(new StringTextComponent(TextFormatting.RED + Configuration.instance().localization.linking.alreadyLinked.replace("%player%", Variables.discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromBedrockPlayer(ctx.getSource().asPlayer().getUniqueID())).getAsTag())), false);
                    return 0;
                }
                final int r = Variables.discord_instance.genLinkNumber(ctx.getSource().asPlayer().getUniqueID());
                ctx.getSource().sendFeedback(TextComponentUtils.func_240648_a_(new StringTextComponent(Configuration.instance().localization.linking.linkMsgIngame.replace("%num%", r + "").replace("%prefix%", "/")), Style.EMPTY.setFormatting(TextFormatting.AQUA).setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "" + r)).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(Configuration.instance().localization.linking.hoverMsg_copyClipboard)))), false);
            } else {
                ctx.getSource().sendFeedback(new StringTextComponent(TextFormatting.RED + Configuration.instance().localization.commands.subcommandDisabled), false);
            }
            return 0;
        })).then(Commands.literal("stop").requires((p) -> p.hasPermissionLevel(4)).executes((ctx) -> {
            Variables.discord_instance.kill();
            ctx.getSource().sendFeedback(new StringTextComponent("DiscordIntegration was successfully stopped!"), false);
            return 0;
        })).then(Commands.literal("reload").requires((p) -> p.hasPermissionLevel(4)).executes((ctx) -> {
            try {
                Configuration.instance().loadConfig();
            } catch (IOException e) {
                ctx.getSource().sendFeedback(new StringTextComponent(e.getMessage()).setStyle(Style.EMPTY.setFormatting(TextFormatting.RED)),true);
                e.printStackTrace();
            }
            AddonLoader.reloadAll();
            ctx.getSource().sendFeedback(new StringTextComponent(Configuration.instance().localization.commands.configReloaded), true);
            return 0;
        }));
        dispatcher.register(l);
    }
}
