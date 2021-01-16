package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class CommandFromCFG extends DiscordCommand {
    private final String cmd, desc, mcCmd, argText;
    private final boolean admin;
    private final String[] aliases;
    private final boolean useArgs;
    private final String[] channelIDs;

    public CommandFromCFG(@Nonnull String cmd, @Nonnull String description, @Nonnull String mcCommand, boolean adminOnly, @Nonnull String[] aliases, boolean useArgs, @Nonnull String argText, @Nonnull String[] channelIDs) {
        super("");
        this.channelIDs = channelIDs;
        this.isConfigCmd = true;
        this.desc = description;
        this.cmd = cmd;
        this.admin = adminOnly;
        this.mcCmd = mcCommand;
        this.aliases = aliases;
        this.useArgs = useArgs;
        this.argText = argText;
    }

    @Override
    public boolean worksInChannel(String channelID) {
        return Arrays.equals(channelIDs, new String[]{"00"}) || Arrays.equals(channelIDs, new String[]{"0"}) && channelID.equals(Configuration.instance().general.botChannel) || ArrayUtils.contains(channelIDs, channelID);
    }

    /**
     * Sets the name of the command
     */
    @Override
    public String getName() {
        return cmd;
    }

    @Override
    public boolean adminOnly() {
        return admin;
    }

    /**
     * Sets the aliases of the command
     */
    @Override
    public String[] getAliases() {
        return aliases;
    }

    /**
     * Sets the description for the help command
     */
    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public String getCommandUsage() {
        if (useArgs) return super.getCommandUsage() + " " + argText;
        else return super.getCommandUsage();
    }

    /**
     * Method called when executing this command
     *
     * @param args   arguments passed by the player
     * @param cmdMsg the {@link MessageReceivedEvent} of the message
     */
    @Override
    public void execute(String[] args, final MessageReceivedEvent cmdMsg) {
        String cmd = mcCmd;
        String argString = "";
        int argsCount = useArgs ? args.length : 0;
        if (argsCount > 0) {
            for (int i = 0; i < argsCount; i++) {
                argString += (" " + args[i]);
            }
        }
        if (!cmd.contains("%args%")) cmd = cmd + argString;
        else cmd = cmd.replace("%args%", argString.trim());
        discord_instance.srv.runMcCommand(cmd, cmdMsg);
    }

}
