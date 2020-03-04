package de.erdbeerbaerlp.dcintegration.commands;

import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.storage.Configuration;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;


public class CommandUptime extends DiscordCommand {
    public CommandUptime() {
        super(Configuration.INSTANCE.uptimeCmdChannelID.get());
    }

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
    public void execute(String[] args, final MessageReceivedEvent cmdMsg) {
        discord.sendMessage("The server is running for " + DiscordIntegration.getFullUptime(), cmdMsg.getTextChannel());
    }
}
