package de.erdbeerbaerlp.dcintegration.common.util;

import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public abstract class ServerInterface {

    public abstract int getMaxPlayers();

    public abstract int getOnlinePlayers();

    public abstract void sendMCMessage(Component msg);

    public abstract String formatEmoteMessage(List<Emote> emotes, String msg);

    public abstract void sendMCReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, MessageReaction.ReactionEmote reactionEmote);

    public abstract void runMcCommand(String cmd, MessageReceivedEvent msgEvent);

    public abstract HashMap<UUID, String> getPlayers();

    public abstract void sendMCMessage(String msg, UUID player);

    public abstract boolean isOnlineMode();

    public abstract String getNameFromUUID(UUID uuid);
}
