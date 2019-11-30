package de.erdbeerbaerlp.dcintegration.commands;

import de.erdbeerbaerlp.dcintegration.Configuration;
import de.erdbeerbaerlp.dcintegration.Discord;
import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;


/**
 * Abstract class used for discord commands
 */
public abstract class DiscordCommand
{
    /**
     * Instance of {@link MinecraftServer}
     */
    final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
    /**
     * Discord instance for easy use in commands
     */
    public Discord discord = DiscordIntegration.discord_instance;
    /**
     * The channel ID the command listens to
     */
    private final String[] channelIDs;
    boolean isConfigCmd = false;
    
    protected DiscordCommand(String[] channelIDs) {this.channelIDs = channelIDs;}
    
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
    public final boolean worksInChannel(String channelID) {
        return Arrays.equals(this.channelIDs, new String[]{"00"}) || Arrays.equals(this.channelIDs, new String[]{"0"}) || ArrayUtils.contains(channelIDs, channelID);
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
     * Should the user be able to execute this command
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
        return Configuration.COMMANDS.CMD_PREFIX + getName();
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
        return Configuration.COMMANDS.MSG_PLAYER_NOT_FOUND.replace("%player%", playerName);
    }
    
    public boolean isConfigCommand() {
        return isConfigCmd;
    }
}
