package de.erdbeerbaerlp.dcintegration.commands;

import de.erdbeerbaerlp.dcintegration.storage.Configuration;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;


public class CommandHelp extends DiscordCommand {

    public CommandHelp() {
        super(Configuration.INSTANCE.helpCmdChannelID.get());
    }

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
        StringBuilder out = new StringBuilder(Configuration.INSTANCE.helpHeader.get() + " \n```\n");
        for (final DiscordCommand cmd : discord.getCommandList()) {
            if (cmd.canUserExecuteCommand(cmdMsg.getAuthor()) && cmd.includeInHelp() && cmd.worksInChannel(cmdMsg.getTextChannel()))
                out.append(cmd.getCommandUsage()).append(" - ").append(cmd.getDescription()).append("\n");
            else
                out.append("[NO PERMS] ").append(cmd.getCommandUsage()).append(" - ").append(cmd.getDescription()).append("\n");
        }
        discord.sendMessage(out + "\n```", cmdMsg.getTextChannel());

    }
}
