package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.discordCommands.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class CommandHelp extends DiscordCommand {

    public CommandHelp() {
        super(Configuration.instance().advanced.helpCmdChannelIDs);
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
        return Configuration.instance().localization.commands.descriptions.help;
    }

    @Override
    public void execute(String[] args, final MessageReceivedEvent cmdMsg) {
        StringBuilder out = new StringBuilder(Configuration.instance().localization.commands.cmdHelp_header + " \n```\n");
        for (final DiscordCommand cmd : CommandRegistry.getCommandList()) {
            if (cmd.canUserExecuteCommand(cmdMsg.getAuthor()) && cmd.includeInHelp() && cmd.worksInChannel(cmdMsg.getTextChannel()))
                out.append(cmd.getCommandUsage()).append(" - ").append(cmd.getDescription()).append("\n");
        }
        discord_instance.sendMessage(out + "\n```", cmdMsg.getTextChannel());

    }
}
