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
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

public class SpigotEventListener implements Listener {
    public static final ArrayList<UUID> timeouts = new ArrayList<>();

    @EventHandler(priority = EventPriority.LOWEST)
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent ev) {
        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueId()).hideFromDiscord) return;
            discord_instance.sendMessage(Configuration.instance().localization.playerJoin.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getPlayer())));

            // Fix link status (if user does not have role, give the role to the user, or vice versa)
            final Thread fixLinkStatus = new Thread(() -> {
                if (Configuration.instance().linking.linkedRoleID.equals("0")) return;
                final UUID uuid = ev.getPlayer().getUniqueId();
                if (!PlayerLinkController.isPlayerLinked(uuid)) return;
                final Guild guild = discord_instance.getChannel().getGuild();
                final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
                final Member member = guild.getMemberById(PlayerLinkController.getDiscordFromPlayer(uuid));
                if (PlayerLinkController.isPlayerLinked(uuid)) {
                    if (!member.getRoles().contains(linkedRole))
                        guild.addRoleToMember(member, linkedRole).queue();
                }
            });
            fixLinkStatus.setDaemon(true);
            fixLinkStatus.start();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent ev) {
        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueId()).hideFromDiscord) return;
            try {
                final Object adv = ev.getAdvancement();
                final Class<?> test = adv.getClass();
                final Field h = test.getDeclaredField("handle");
                h.setAccessible(true);
                Object handle = h.get(adv);
                Class<?> handleClass = handle.getClass();
                final Field d = handleClass.getDeclaredField("display");
                d.setAccessible(true);
                Object display = d.get(handle);
                if (display == null) return;  //Cannot be displayed
                Class<?> displayClass = display.getClass();

                final Field shouldAnnounceToChat = displayClass.getDeclaredField("g");
                shouldAnnounceToChat.setAccessible(true);
                if (!(boolean) shouldAnnounceToChat.get(display)) return;

                final Field titleTxt = displayClass.getDeclaredField("a");
                titleTxt.setAccessible(true);
                Object titleTextComp = titleTxt.get(display);
                Class<?> titleTextCompClass = titleTextComp.getClass();

                final Method getStrTitle = titleTextCompClass.getMethod("getString");
                getStrTitle.setAccessible(true);
                String title = (String) getStrTitle.invoke(titleTextComp, new Object[0]);

                final Field descTxt = displayClass.getDeclaredField("b");
                descTxt.setAccessible(true);
                Object descTextComp = descTxt.get(display);
                Class<?> descTextCompClass = descTextComp.getClass();

                final Method getStrDesc = descTextCompClass.getMethod("getString");
                getStrDesc.setAccessible(true);
                String description = (String) getStrDesc.invoke(descTextComp, new Object[0]);

                /* //Used for finding the required fields and methods
                for (Field s : titleTextCompClass.getDeclaredFields()) {
                    s.setAccessible(true);
                    System.out.println(s.toString() + " : " + s.get(titleTextComp));
                }
                for (Field s : titleTextCompClass.getFields()) {
                    s.setAccessible(true);
                    System.out.println(s.toString() + " : " + s.get(titleTextComp));
                }
                for (Method m : titleTextCompClass.getMethods()) System.out.println(m.toString());*/
                discord_instance.sendMessage(Configuration.instance().localization.advancementMessage.replace("%player%",
                        MessageUtils.removeFormatting(SpigotMessageUtils.formatPlayerName(ev.getPlayer())))
                        .replace("%name%",
                                MessageUtils.removeFormatting(title))
                        .replace("%desc%",
                                MessageUtils.removeFormatting(description))
                        .replace("\\n", "\n"));
            } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }

        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeave(PlayerQuitEvent ev) {
        if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueId()).hideFromDiscord) return;
        if (discord_instance != null && !timeouts.contains(ev.getPlayer().getUniqueId()))
            discord_instance.sendMessage(Configuration.instance().localization.playerLeave.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getPlayer())));
        else if (discord_instance != null && timeouts.contains(ev.getPlayer().getUniqueId())) {
            discord_instance.sendMessage(Configuration.instance().localization.playerTimeout.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getPlayer())));
            timeouts.remove(ev.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent ev) {
        if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueId()).hideFromDiscord) return;
        String command = ev.getMessage().replaceFirst(Pattern.quote("/"), "");
        if (!Configuration.instance().commandLog.channelID.equals("0")) {
            if (!ArrayUtils.contains(Configuration.instance().commandLog.ignoredCommands, command.split(" ")[0]))
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
                    discord_instance.sendMessage(ev.getPlayer().getName(), ev.getPlayer().getUniqueId().toString(), new DiscordMessage(null, msg.trim(), !raw), discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(PlayerDeathEvent ev) {
        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, ev.getEntity().getUniqueId()).hideFromDiscord) return;
            final String deathMessage = ev.getDeathMessage();
            discord_instance.sendMessage(new DiscordMessage(Configuration.instance().localization.playerDeath.replace("%player%", SpigotMessageUtils.formatPlayerName(ev.getEntity())).replace("%msg%", MessageUtils.removeFormatting(deathMessage).replace(ev.getEntity().getName() + " ", ""))), discord_instance.getChannel(Configuration.instance().advanced.deathsChannelID));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent ev) {
        if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueId()).hideFromDiscord) return;
        if (discord_instance != null) {
            if (discord_instance.callEvent((e) -> {
                if (e instanceof SpigotDiscordEventHandler)
                    return ((SpigotDiscordEventHandler) e).onMcChatMessage(ev);
                return false;
            })) return;

            String text = MessageUtils.escapeMarkdown(ev.getMessage().replace("@everyone", "[at]everyone").replace("@here", "[at]here"));
            final TextChannel channel = discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID);
            discord_instance.sendMessage(SpigotMessageUtils.formatPlayerName(ev.getPlayer()), ev.getPlayer().getUniqueId().toString(), new DiscordMessage(null, text, true), channel);

            //Set chat message to a more readable format
            if (channel != null) ev.setMessage(MessageUtils.mentionsToNames(ev.getMessage(), channel.getGuild()));
        }
    }
}
