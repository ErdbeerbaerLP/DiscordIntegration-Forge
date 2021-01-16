package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class CommandList extends DiscordCommand {
    public CommandList() {
        super(Configuration.instance().advanced.listCmdChannelIDs);
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"online"};
    }

    @Override
    public String getDescription() {
        return Configuration.instance().localization.commands.descriptions.list;
    }

    @Override
    public void execute(String[] args, final MessageReceivedEvent cmdMsg) {
        final HashMap<UUID, String> players = discord_instance.srv.getPlayers();
        if (players.isEmpty()) {
            discord_instance.sendMessage(Configuration.instance().localization.commands.cmdList_empty, cmdMsg.getTextChannel());
            return;
        }
        StringBuilder out = new StringBuilder((players.size() == 1 ? Configuration.instance().localization.commands.cmdList_one
                : Configuration.instance().localization.commands.cmdList_header.replace("%amount%", "" + players.size())) + "\n```\n");

        for (Map.Entry<UUID, String> p : players.entrySet()) {
            out.append(discord_instance.srv.getNameFromUUID(p.getKey())).append(",");
        }


        out = new StringBuilder(out.substring(0, out.length() - 1));
        discord_instance.sendMessage(out + "\n```", cmdMsg.getTextChannel());
    }
}
