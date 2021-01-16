package de.erdbeerbaerlp.dcintegration.common.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.kyori.adventure.text.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

public abstract class ServerInterface {

    /**
     * @return Maximum player amount on this server
     */
    public abstract int getMaxPlayers();
    /**
     * @return Online player count on this server
     */
    public abstract int getOnlinePlayers();

    /**
     * Sends an {@link Component} as ingame message to all players who are not ignoring messages
     * @param msg Message to send
     */
    public abstract void sendMCMessage(@Nonnull Component msg);

    /**
     * Sends an message for reactions from discord
     * @param member Discord {@link Member}, who reacted
     * @param retrieveMessage Message which got the reaction
     * @param targetUUID Original sender's {@link UUID}
     * @param reactionEmote Emote that was added to the message
     */
    public abstract void sendMCReaction(@Nonnull Member member, @Nonnull RestAction<Message> retrieveMessage, @Nonnull UUID targetUUID, @Nonnull MessageReaction.ReactionEmote reactionEmote);

    /**
     * Runs an command on the server
     * @param cmd Command to execute
     * @param msgEvent Message event
     */
    public abstract void runMcCommand(@Nonnull String cmd, @Nonnull MessageReceivedEvent msgEvent);

    /**
     * @return all online players on this server in format <{@link UUID}, PlayerName>
     */
    @Nonnull
    public abstract HashMap<UUID, String> getPlayers();

    /**
     * Sends an message to that specific player
     * @param msg Message
     * @param player Target player's {@link UUID}
     */
    public abstract void sendMCMessage(String msg, UUID player);

    /**
     * Checks if the server is running in online mode
     * @return online mode status
     */
    public abstract boolean isOnlineMode();

    /**
     * Gets the display name of the player's UUID
     * @param uuid {@link UUID} to get the name from
     * @return The player's name, or null if the player was not found
     */
    @Nullable
    public abstract String getNameFromUUID(@Nonnull UUID uuid);
}
