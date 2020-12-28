package de.erdbeerbaerlp.dcintegration.spigot.command;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.DiscordIntegration;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class McDiscordCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Configuration.instance().localization.commands.ingameOnly);
            return true;
        }
        final Player p = (Player) sender;
        final BukkitAudiences bukkitAudiences = BukkitAudiences.create(DiscordIntegration.INSTANCE);
        if (args.length == 0) {
            bukkitAudiences.player(p).sendMessage(Component.text(Configuration.instance().ingameCommand.message).style(Style.empty().clickEvent(ClickEvent.openUrl(Configuration.instance().ingameCommand.inviteURL)).hoverEvent(HoverEvent.showText(Component.text(Configuration.instance().ingameCommand.hoverMessage)))));
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "link":
                    if (Configuration.instance().linking.enableLinking && Variables.discord_instance.srv.isOnlineMode() && !Configuration.instance().linking.whitelistMode) {
                        if (PlayerLinkController.isPlayerLinked(p.getUniqueId())) {
                            bukkitAudiences.player(p).sendMessage(Component.text(Configuration.instance().localization.linking.alreadyLinked.replace("%player%", PlayerLinkController.getDiscordFromPlayer(p.getUniqueId()))).style(Style.style(TextColors.of(Color.RED))));
                            break;
                        }
                        final int r = Variables.discord_instance.genLinkNumber(p.getUniqueId());
                        bukkitAudiences.player(p).sendMessage(Component.text(Configuration.instance().localization.linking.linkMsgIngame.replace("%num%", r + "").replace("%prefix%", Configuration.instance().commands.prefix)).style(Style.style(TextColors.of(Color.ORANGE)).clickEvent(ClickEvent.copyToClipboard(Configuration.instance().commands.prefix + "link " + r)).hoverEvent(HoverEvent.showText(Component.text(Configuration.instance().localization.linking.hoverMsg_copyClipboard)))));
                    } else {
                        bukkitAudiences.player(p).sendMessage(Component.text(Configuration.instance().localization.commands.subcommandDisabled).style(Style.style(TextColors.of(Color.RED))));
                    }
                    break;
                case "ignore":
                    bukkitAudiences.player(p).sendMessage(Component.text(Variables.discord_instance.togglePlayerIgnore(p.getUniqueId()) ? Configuration.instance().localization.commands.commandIgnore_unignore : Configuration.instance().localization.commands.commandIgnore_ignore));
                    break;
                case "restart":
                    if (p.hasPermission("dcintegration.admin"))
                        new Thread(() -> {
                            if (Variables.discord_instance.restart())
                                bukkitAudiences.player(p).sendMessage(Component.text("Discord Bot restarted"));
                            else
                                bukkitAudiences.player(p).sendMessage(Component.text("Failed to properly restart the discord bot!").style(Style.style(TextColors.of(Color.RED))));
                        }).start();
                    else
                        bukkitAudiences.player(p).sendMessage(Component.text(Configuration.instance().localization.commands.noPermission).style(Style.style(TextColors.of(Color.RED))));
                    break;
                case "reload":
                    if (p.hasPermission("dcintegration.admin")) {
                        Configuration.instance().loadConfig();
                        bukkitAudiences.player(p).sendMessage(Component.text(Configuration.instance().localization.commands.configReloaded));
                    } else {
                        bukkitAudiences.player(p).sendMessage(Component.text(Configuration.instance().localization.commands.noPermission).style(Style.style(TextColors.of(Color.RED))));
                    }
                    break;
            }
        } else {
            bukkitAudiences.player(p).sendMessage(Component.text(Configuration.instance().localization.commands.tooManyArguments).style(Style.style(TextColors.of(Color.RED))));
        }
        return true;
    }

    public static class TabCompleter implements org.bukkit.command.TabCompleter {

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
            final ArrayList<String> out = new ArrayList<>();
            if (args.length == 1) {
                final ArrayList<String> cmds = new ArrayList<>();
                cmds.add("link");
                cmds.add("ignore");
                if (sender.hasPermission("dcintegration.admin")) {
                    cmds.add("reload");
                    cmds.add("restart");
                }
                if (!args[0].isEmpty())
                    for (String cmd : cmds) {
                        if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                            out.add(cmd);
                        }
                    }
                else out.addAll(cmds);
            }
            return out;
        }
    }
}
