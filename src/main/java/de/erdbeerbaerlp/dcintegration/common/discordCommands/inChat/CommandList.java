package de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.forge.util.ForgeMessageUtils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

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
        return "Lists all players currently online";
    }

    @Override
    public void execute(String[] args, final MessageReceivedEvent cmdMsg) {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server.getPlayerList().getPlayers().isEmpty()) {
            discord_instance.sendMessage(Configuration.instance().localization.cmdList_empty, cmdMsg.getTextChannel());
            return;
        }
        String out = (server.getPlayerList().getPlayers().size() == 1 ? Configuration.instance().localization.cmdList_one
                : Configuration.instance().localization.cmdList_header.replace("%amount%", "" + server.getPlayerList().getPlayers().size())) + "\n```\n";
        for (final ServerPlayerEntity p : server.getPlayerList().getPlayers()) {
            out += ForgeMessageUtils.formatPlayerName(p) + ",";


        }
        out = out.substring(0, out.length() - 1);
        discord_instance.sendMessage(out + "\n```", cmdMsg.getTextChannel());
    }
}
