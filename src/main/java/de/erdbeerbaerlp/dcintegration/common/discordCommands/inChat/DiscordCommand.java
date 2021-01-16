package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


/**
 * Abstract class used for discord commands
 */
public abstract class DiscordCommand {

    private final String[] EVERYWHERE = new String[]{"00"};
    private final String[] ONLY_IN_BOT_CHANNEL = new String[]{"0"};
    /**
     * The channel ID the command listens to
     */
    private final String[] channelIDs;
    boolean isConfigCmd = false;

    protected DiscordCommand(@Nonnull String[] channelIDs) {
        this.channelIDs = channelIDs;
    }

    protected DiscordCommand(@Nonnull String channelID) {
        this.channelIDs = new String[]{channelID};
    }

    /**
     * Checks if this command works from this channel
     *
     * @param channel TextChannel to check for
     */
    public final boolean worksInChannel(final TextChannel channel) {
        if(channel == null) return false;
        return worksInChannel(channel.getId());
    }

    /**
     * Checks if this command works from this channel
     *
     * @param channelID Channel ID of the current channel
     */
    public boolean worksInChannel(@Nonnull String channelID) {
        if (Arrays.equals(channelIDs, EVERYWHERE)) return true;
        if (Arrays.equals(channelIDs, ONLY_IN_BOT_CHANNEL))
            return channelID.equals(Configuration.instance().general.botChannel);
        return ArrayUtils.contains(channelIDs, channelID);
    }

    /**
     * Sets the name of the command
     */
    @Nonnull
    public abstract String getName();

    /**
     * Sets the aliases of the command
     */
    @Nonnull
    public abstract String[] getAliases();

    /**
     * Sets the description for the help command
     */
    @Nonnull
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
    public abstract void execute(@Nonnull String[] args, @Nonnull final MessageReceivedEvent cmdMsg);

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
    public boolean canUserExecuteCommand(@Nonnull User user) {
        Member m = null;
        for (final Member me : discord_instance.getChannel().getMembers()) {
            if (me.getUser().equals(user)) {
                m = me;
                break;
            }
        }
        if (m == null) return false;
        return !this.adminOnly() || discord_instance.hasAdminRole(m.getRoles());
    }


    /**
     * Override to customize the command usage, which is being displayed in help (ex. to add arguments)
     */
    public String getCommandUsage() {
        return Configuration.instance().commands.prefix + getName();
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
    public final String getPlayerNotFoundMsg(String playerName) {
        return Configuration.instance().localization.commands.playerNotFound.replace("%player%", playerName);
    }

    public final boolean isConfigCommand() {
        return isConfigCmd;
    }
}
