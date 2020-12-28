package de.erdbeerbaerlp.dcintegration.spigot.util;

import com.vdurmont.emoji.EmojiParser;
import de.erdbeerbaerlp.dcintegration.common.DiscordEventListener;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.ServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.DiscordIntegration;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class SpigotServerInterface extends ServerInterface {
    @Override
    public int getMaxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    @Override
    public int getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public void sendMCMessage(Component msg) {
        final BukkitAudiences bukkitAudiences = BukkitAudiences.create(DiscordIntegration.INSTANCE);
        final Collection<? extends Player> l = Bukkit.getOnlinePlayers();
        msg = msg.replaceText(MessageUtils.replaceLiteral("\\\n", Component.newline()));
        try {
            for (final Player p : l) {
                if (!Variables.discord_instance.ignoringPlayers.contains(p.getUniqueId()) && !(PlayerLinkController.isPlayerLinked(p.getUniqueId()) && PlayerLinkController.getSettings(null, p.getUniqueId()).ignoreDiscordChatIngame)) {
                    final Map.Entry<Boolean, Component> ping = MessageUtils.parsePing(msg, p.getUniqueId(), p.getName());
                    bukkitAudiences.player(p).sendMessage(msg);
                    if (ping.getKey()) {
                        if (PlayerLinkController.getSettings(null, p.getUniqueId()).pingSound)
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                    }
                }
            }
            //Send to server console too
            bukkitAudiences.sender(Bukkit.getConsoleSender()).sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String formatEmoteMessage(List<Emote> emotes, String msg) {
        msg = EmojiParser.parseToAliases(msg);
        for (final Emote e : emotes) {
            msg = msg.replace(e.toString(), ":" + e.getName() + ":");
        }
        return msg;
    }

    @Override
    public void sendMCReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, MessageReaction.ReactionEmote reactionEmote) {
        final Collection<? extends Player> l = Bukkit.getOnlinePlayers();
        for (final Player p : l) {
            if (p.getUniqueId().equals(targetUUID) && !Variables.discord_instance.ignoringPlayers.contains(p.getUniqueId()) && !(PlayerLinkController.isPlayerLinked(p.getUniqueId()) && PlayerLinkController.getSettings(null, p.getUniqueId()).ignoreDiscordChatIngame)) {
                final String emote = reactionEmote.isEmote() ? ":" + reactionEmote.getEmote().getName() + ":" : reactionEmote.getEmoji();
                String outMsg = Configuration.instance().localization.reactionMessage.replace("%name%", member.getEffectiveName()).replace("%name2%", member.getUser().getAsTag()).replace("%emote%", emote);
                if (Configuration.instance().localization.reactionMessage.contains("%msg%"))
                    retrieveMessage.submit().thenAccept((m) -> {
                        String outMsg2 = outMsg.replace("%msg%", m.getContentDisplay());
                        sendReactionMCMessage(p, formatEmoteMessage(m.getEmotes(), outMsg2));
                    });
                else sendReactionMCMessage(p, outMsg);
            }
        }
    }

    private void sendReactionMCMessage(Player target, String msg) {
        final BukkitAudiences bukkitAudiences = BukkitAudiences.create(DiscordIntegration.INSTANCE);
        final Component msgComp = MinecraftSerializer.INSTANCE.serialize(msg.replace("\n", "\\n"), DiscordEventListener.mcSerializerOptions);
        msgComp.replaceText("\\\\n", Component.newline());
        bukkitAudiences.player(target).sendMessage(msgComp);
    }

    @Override
    public void runMcCommand(String cmd, MessageReceivedEvent msgEvent) {
        Bukkit.getScheduler().runTask(DiscordIntegration.INSTANCE, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

    @Override
    public HashMap<UUID, String> getPlayers() {
        final HashMap<UUID, String> players = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            players.put(p.getUniqueId(), p.getName());
        }
        return players;
    }

    @Override
    public void sendMCMessage(String msg, UUID player) {
        Bukkit.getPlayer(player).sendMessage(msg);
    }

    @Override
    public String getNameFromUUID(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    @Override
    public boolean isOnlineMode() {
        return Bukkit.getOnlineMode();
    }
}
