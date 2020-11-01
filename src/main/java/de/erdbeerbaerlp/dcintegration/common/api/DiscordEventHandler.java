package de.erdbeerbaerlp.dcintegration.common.api;

import de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat.DiscordCommand;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs.DMCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class DiscordEventHandler {
    /**
     * Gets called when someone DMs the bot before any code gets executed
     *
     * @return true to cancel default code execution
     */
    public abstract boolean onDiscordPrivateMessage(final MessageReceivedEvent event);

    /**
     * Gets called on discord message in any channel other than private before anything processed (like commands)
     *
     * @return true to cancel default code execution
     */
    public abstract boolean onDiscordMessagePre(final MessageReceivedEvent event);

    /**
     * Gets called when an command was entered, invalid or not
     *
     * @param command the executed command or null if the command was invalid, or the user had no permission for any command
     * @return true to cancel default code execution
     */
    public abstract boolean onDiscordCommand(final MessageReceivedEvent event, @Nullable final DiscordCommand command);

    /**
     * Gets called when an DM command was entered, invalid or not
     *
     * @param command the executed command or null if the command was invalid, or the user had no permission for any command
     * @return true to cancel default code execution
     */
    public abstract boolean onDiscordDMCommand(final MessageReceivedEvent event, @Nullable final DMCommand command);

    /**
     * Gets called after command execution or message forwarding in any channel
     */
    public abstract void onDiscordMessagePost(final MessageReceivedEvent event);

    /**
     * Gets called when an player successfully links their Discord and Minecraft account
     */
    public abstract void onPlayerLink(final UUID mcUUID, final String discordID);
}
