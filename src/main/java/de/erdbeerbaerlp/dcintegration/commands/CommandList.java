package de.erdbeerbaerlp.dcintegration.commands;

import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.math.Ticks;
import com.feed_the_beast.ftbutilities.FTBUtilitiesConfig;
import com.feed_the_beast.ftbutilities.data.FTBUtilitiesPlayerData;

import de.erdbeerbaerlp.dcintegration.Configuration;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;

public class CommandList extends DiscordCommand {

	@Override
	public String getName() {
		return "list";
	}

	@Override
	public String[] getAliases() {
		return new String[0];
	}

	@Override
	public String getDescription() {
		return "Lists all players currently online";
	}

	@Override
	public void execute(String[] args, MessageReceivedEvent cmdMsg) {
		if(server.getPlayerList().getPlayers().isEmpty()) {
			discord.sendMessage(Configuration.COMMANDS.MSG_LIST_EMPTY);
			return;
		}
		String out = "There are "+server.getPlayerList().getPlayers().size()+" online players!\n```\n";
		if(!Loader.isModLoaded("ftbutilities") || !FTBUtilitiesConfig.afk.enabled)
			for(final EntityPlayerMP p : server.getPlayerList().getPlayers()) {
				out = out+p.getName()+",";
			}
		else {
			final Universe universe = Universe.get();
			for(final EntityPlayerMP p : server.getPlayerList().getPlayers()) {
				final FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(universe.getPlayer(p));
				final boolean afk = data.afkTime >= Ticks.get(FTBUtilitiesConfig.afk.notification_timer).millis();
				out = out+(afk?"[AFK]":"")+p.getName()+",";
			}
			out = out.substring(0, out.length()-1);
		}
		discord.sendMessage(out+"\n```");
	}

}
