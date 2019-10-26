package de.erdbeerbaerlp.dcintegration.commands;

import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;


public class CommandUptime extends DiscordCommand
{
    
    @Override
    public String getName() {
        return "uptime";
    }
    
    @Override
    public String[] getAliases() {
        return new String[]{"up"};
    }
    
    @Override
    public String getDescription() {
        return "Displays the server uptime";
    }
    
    @Override
    public void execute(String[] args, MessageReceivedEvent cmdMsg) {
        discord.sendMessage("The server is running for " + DiscordIntegration.getUptime());
    }
    
}
