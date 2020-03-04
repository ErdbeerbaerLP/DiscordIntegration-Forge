package de.erdbeerbaerlp.dcintegration.commands;

import de.erdbeerbaerlp.dcintegration.Discord;
import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.storage.Configuration;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;


/**
 * Abstract class used for discord commands
 */
public abstract class DiscordCommand {
    /**
     * Instance of {@link MinecraftServer}
     */
    final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    /**
     * The channel ID the command listens to
     */
    private final String channelID;
    /**
     * Discord instance for easy use in commands
     */
    public Discord discord = DiscordIntegration.discord_instance;
    boolean isConfigCmd = false;

    protected DiscordCommand(String channelID) {
        this.channelID = channelID;
    }

    /**
     * Checks if this command works from this channel
     *
     * @param channel TextChannel to check for
     */
    public final boolean worksInChannel(final TextChannel channel) {
        return worksInChannel(channel.getId());
    }

    /**
     * Checks if this command works from this channel
     *
     * @param channelID Channel ID of the current channel
     */
    public boolean worksInChannel(String channelID) {
        return this.channelID.equals("00") || (this.channelID.equals("0") && channelID.equals(Configuration.INSTANCE.botChannel.get())) || this.channelID.equals(channelID);
    }

    /**
     * Sets the name of the command
     */
    public abstract String getName();

    /**
     * Sets the aliases of the command
     */
    public abstract String[] getAliases();

    /**
     * Sets the description for the help command
     */
    public abstract String getDescription();

    /**
     * Is this command only for admins?
     */
    public boolean adminOnly() {
        return false;
    }

    /**
     * Method called when executing this command
     * <p>
     *
     * @param args   arguments passed by the player
     * @param cmdMsg the {@link MessageReceivedEvent} of the message
     */
    public abstract void execute(String[] args, final MessageReceivedEvent cmdMsg);

    /**
     * Wether or not this command should be visible in help
     */
    public boolean includeInHelp() {
        return true;
    }

    /**
     * Should the user be able to execute this command?
     * <p>
     *
     * @param user The user being handled
     * @return wether or not the user can execute this command
     */
    public boolean canUserExecuteCommand(User user) {
        Member m = null;
        for (final Member me : discord.getChannel().getMembers()) {
            if (me.getUser().equals(user)) {
                m = me;
                break;
            }
        }
        if (m == null) return false;
        return !this.adminOnly() || m.getRoles().contains(discord.getAdminRole());
    }


    /**
     * Override to customize the command usage, which is being displayed in help (ex. to add arguments)
     */
    public String getCommandUsage() {
        return Configuration.INSTANCE.prefix.get() + getName();
    }

    public final boolean equals(DiscordCommand cmd) {
        return cmd.getName().equals(this.getName());
    }


    /**
     * Generates an Player not found message to send to discord
     *
     * @param playerName Name of the player
     * @return The message
     */
    public final String parsePlayerNotFoundMsg(String playerName) {
        return Configuration.INSTANCE.msgPlayerNotFound.get().replace("%player%", playerName);
    }

    public final boolean isConfigCommand() {
        return isConfigCmd;
    }
}
