package de.erdbeerbaerlp.dcintegration.spigot.compat;

import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat.DiscordCommand;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs.DMCommand;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapWebChatEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DynmapListener extends DiscordEventHandler implements Listener {
    @EventHandler
    public void onDynmapChat(DynmapWebChatEvent event) {

        Variables.discord_instance.sendMessage(Variables.discord_instance.getChannel(Configuration.instance().dynmap.dynmapChannelID), Configuration.instance().dynmap.dcMessage.replace("%msg%", event.getMessage()).replace("%sender%", event.getName().isEmpty() ? Configuration.instance().dynmap.unnamed : event.getName()), Configuration.instance().dynmap.avatarURL, Configuration.instance().dynmap.name);
    }


    @Override
    public boolean onDiscordPrivateMessage(MessageReceivedEvent event) {
        return false;
    }

    @Override
    public boolean onDiscordMessagePre(MessageReceivedEvent event) {
        return false;
    }

    @Override
    public boolean onDiscordCommand(MessageReceivedEvent event, @Nullable DiscordCommand command) {
        return false;
    }

    @Override
    public boolean onDiscordDMCommand(MessageReceivedEvent event, @Nullable DMCommand command) {
        return false;
    }

    @Override
    public void onDiscordMessagePost(MessageReceivedEvent event) {
        final DynmapCommonAPI api = (DynmapCommonAPI) Bukkit.getPluginManager().getPlugin("dynmap");
        api.sendBroadcastToWeb(Configuration.instance().dynmap.webName.replace("%name#tag%", event.getAuthor().getAsTag()).replace("%name%", event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName()), event.getMessage().getContentDisplay());
    }

    @Override
    public void onPlayerLink(UUID mcUUID, String discordID) {

    }
}
