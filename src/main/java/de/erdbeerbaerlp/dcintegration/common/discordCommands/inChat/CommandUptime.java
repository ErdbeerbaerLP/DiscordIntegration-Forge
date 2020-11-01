package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class CommandUptime extends DiscordCommand {
    public CommandUptime() {
        super(Configuration.instance().advanced.uptimeCmdChannelIDs);
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
        discord_instance.sendMessage("The server is running for " + MessageUtils.getFullUptime(), cmdMsg.getTextChannel());
    }
}
