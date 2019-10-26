package de.erdbeerbaerlp.dcintegration.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;


public class CommandHelp extends DiscordCommand
{
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
    public void execute(String[] args, MessageReceivedEvent cmdMsg) {
        String out = "All available commands: \n```\n";
        for (DiscordCommand cmd : discord.getCommandList()) {
            if (cmd.canUserExecuteCommand(cmdMsg.getAuthor()) && cmd.includeInHelp()) out = out + cmd.getCommandUsage() + " - " + cmd.getDescription() + "\n";
        }
        discord.sendMessage(out + "\n```");
        
    }
}
