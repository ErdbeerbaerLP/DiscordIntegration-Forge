package de.erdbeerbaerlp.dcintegration.spigot;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.api.SpigotDiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

public class SpigotEventListener implements Listener {
    public static final ArrayList<UUID> timeouts = new ArrayList<>();

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent ev) {
        if (Configuration.instance().linking.whitelistMode && discord_instance.srv.isOnlineMode()) {
            try {
                if (!PlayerLinkController.isPlayerLinked(ev.getPlayer().getUniqueId())) {
                    ev.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Configuration.instance().localization.linking.notWhitelisted);
                }
            } catch (IllegalStateException e) {
                ev.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Please check " + Variables.discordDataDir + "LinkedPlayers.json\n\n" + e.toString());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev) {
        if (discord_instance != null) {
            discord_instance.sendMessage(Configuration.instance().localization.playerJoin.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getPlayer())));

            // Fix link status (if user does not have role, give the role to the user, or vice versa)
            final Thread fixLinkStatus = new Thread(() -> {
                if (Configuration.instance().linking.linkedRoleID.equals("0")) return;
                final UUID uuid = ev.getPlayer().getUniqueId();
                if (!PlayerLinkController.isPlayerLinked(uuid)) return;
                final Guild guild = discord_instance.getChannel().getGuild();
                final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
                final Member member = guild.getMember(discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromPlayer(uuid)));
                if (PlayerLinkController.isPlayerLinked(uuid)) {
                    if (!member.getRoles().contains(linkedRole))
                        guild.addRoleToMember(member, linkedRole).queue();
                }
            });
            fixLinkStatus.setDaemon(true);
            fixLinkStatus.start();
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent ev) {
        if (discord_instance != null && !timeouts.contains(ev.getPlayer().getUniqueId()))
            discord_instance.sendMessage(Configuration.instance().localization.playerLeave.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getPlayer())));
        else if (discord_instance != null && timeouts.contains(ev.getPlayer().getUniqueId())) {
            discord_instance.sendMessage(Configuration.instance().localization.playerTimeout.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getPlayer())));
            timeouts.remove(ev.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent ev) {
        String command = ev.getMessage().replaceFirst(Pattern.quote("/"), "");
        if (!Configuration.instance().commandLog.channelID.equals("0")) {
            discord_instance.sendMessage(Configuration.instance().commandLog.message
                    .replace("%sender%", ev.getPlayer().getName())
                    .replace("%cmd%", command)
                    .replace("%cmd-no-args%", command.split(" ")[0]), discord_instance.getChannel(Configuration.instance().commandLog.channelID));
        }
        if (discord_instance != null) {
            boolean raw = false;
            if (((command.startsWith("say")) && Configuration.instance().messages.sendOnSayCommand) || ((command.startsWith("me")) && Configuration.instance().messages.sendOnMeCommand)) {
                String msg = command;
                if (command.startsWith("say"))
                    msg = msg.replaceFirst("say", "");
                if (command.startsWith("me")) {
                    raw = true;
                    msg = "*" + MessageUtils.escapeMarkdown(msg.replace("me", "").trim()) + "*";
                }
                if (!msg.trim().isEmpty())
                    discord_instance.sendMessage(ev.getPlayer().getName(), ev.getPlayer().getUniqueId().toString(), new DiscordMessage(null, msg.trim(), !raw), Configuration.instance().advanced.chatOutputChannelID.equals("default") ? discord_instance.getChannel() : discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID));
            }
        }
    }

    @EventHandler
    public void onEntityDeath(PlayerDeathEvent ev) {
        if (discord_instance != null) {
            final String deathMessage = ev.getDeathMessage();
            discord_instance.sendMessage(new DiscordMessage(Configuration.instance().localization.playerDeath.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getEntity())).replace("%msg%", MessageUtils.removeFormatting(deathMessage).replace(ev.getEntity().getName() + " ", ""))), Configuration.instance().advanced.deathsChannelID.equals("default") ? discord_instance.getChannel() : discord_instance.getChannel(Configuration.instance().advanced.deathsChannelID));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent ev) {
        if (discord_instance.callEvent((e) -> {
            if (e instanceof SpigotDiscordEventHandler)
                return ((SpigotDiscordEventHandler) e).onMcChatMessage(ev);
            return false;
        })) return;
        String text = MessageUtils.escapeMarkdown(ev.getMessage().replace("@everyone", "[at]everyone").replace("@here", "[at]here"));
        if (discord_instance != null) {
            discord_instance.sendMessage(SpigotMessageUtils.formatPlayerName(ev.getPlayer()), ev.getPlayer().getUniqueId().toString(), new DiscordMessage(null, text, true), Configuration.instance().advanced.chatOutputChannelID.equals("default") ? discord_instance.getChannel() : discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID));
        }
    }
}
