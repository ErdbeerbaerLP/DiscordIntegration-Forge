package de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs;

import de.erdbeerbaerlp.dcintegration.common.discordCommands.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DMHelpCommand extends DMCommand {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"?", "h"};
    }

    @Override
    public String getDescription() {
        return "Displays a list of all commands";
    }

    @Override
    public void execute(String[] args, final MessageReceivedEvent cmdMsg) {
        StringBuilder out = new StringBuilder(Configuration.instance().localization.cmdHelp_header + " \n```\n");
        for (final DMCommand cmd : CommandRegistry.getDMCommandList()) {
            if (cmd.canUserExecuteCommand(cmdMsg.getAuthor()) && cmd.includeInHelp())
                out.append(cmd.getCommandUsage()).append(" - ").append(cmd.getDescription()).append("\n");
            else
                out.append("[NO PERMS] ").append(cmd.getCommandUsage()).append(" - ").append(cmd.getDescription()).append("\n");
        }
        cmdMsg.getChannel().sendMessage(out + "\n```").queue();

    }

    @Override
    public boolean requiresLink() {
        return false;
    }
}
