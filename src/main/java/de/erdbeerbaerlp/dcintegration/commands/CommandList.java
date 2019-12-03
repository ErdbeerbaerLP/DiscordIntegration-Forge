package de.erdbeerbaerlp.dcintegration.commands;

import de.erdbeerbaerlp.dcintegration.Configuration;
import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;


public class CommandList extends DiscordCommand
{
    public CommandList() {
        super(Configuration.COMMANDS.LIST_CMD_CHANNEL_ID);
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
            discord.sendMessage(Configuration.COMMANDS.MSG_LIST_EMPTY, cmdMsg.getTextChannel());
            return;
        }
        String out = (server.getPlayerList().getPlayers().size() == 1 ? Configuration.INSTANCE.msgListOne.get() : Configuration.INSTANCE.msgListHeader.get().replace("%amount%", "" + server.getPlayerList().getPlayers().size())) + "\n```\n";
//		if(!Loader.isModLoaded("ftbutilities") || !FTBUtilitiesConfig.afk.enabled)
        for (final ServerPlayerEntity p : server.getPlayerList().getPlayers()) {
            out = out + DiscordIntegration.formatPlayerName(p) + ",";
//			}
//		else {
//			final Universe universe = Universe.get();
//			for(final EntityPlayerMP p : server.getPlayerList().getPlayers()) {
//				final FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(universe.getPlayer(p));
//				final boolean afk = data.afkTime >= Ticks.get(FTBUtilitiesConfig.afk.notification_timer).millis();
//				out = out+(afk?"[AFK]":"")+DiscordIntegration.formatPlayerName(p)+",";
//			}
    
        }
        out = out.substring(0, out.length() - 1);
        discord.sendMessage(out + "\n```", cmdMsg.getTextChannel());
    }
}
