package de.erdbeerbaerlp.dcintegration.commands;

import de.erdbeerbaerlp.dcintegration.Configuration;
import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;


public class CommandList extends DiscordCommand
{
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
    public void execute(String[] args, MessageReceivedEvent cmdMsg) {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        System.out.println(server);
        System.out.println(server.getPlayerList());
        System.out.println(server.getPlayerList().getPlayers());
        System.out.println(server.getPlayerList().getPlayers().isEmpty());
        if (server.getPlayerList().getPlayers().isEmpty()) {
            discord.sendMessage(Configuration.INSTANCE.msgListEmpty.get());
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
        discord.sendMessage(out + "\n```");
    }
}
