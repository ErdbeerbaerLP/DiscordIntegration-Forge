package de.erdbeerbaerlp.dcintegration.commands;

import de.erdbeerbaerlp.dcintegration.Configuration;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public class CommandKick extends DiscordCommand {

	@Override
	public String getName() {
		return "kick";
	}

	@Override
	public String[] getAliases() {
		return new String[0];
	}

	@Override
	public String getDescription() {
		return "Kicks an player";
	}
	@Override
	public boolean adminOnly() {
		return true;
	}
	@Override
	public void execute(String[] args, MessageReceivedEvent cmdMsg) {
		if(args.length <= 0 || args[0].isEmpty()) discord.sendMessage(Configuration.COMMANDS.MSG_NOT_ENOUGH_ARGUMENTS);
		else {
			for(EntityPlayerMP player : server.getPlayerList().getPlayers()) {
				if(player.getName().equalsIgnoreCase(args[0])) {
					String kickString = "";
					for(int i=1;i<args.length;i++) {
						kickString = kickString+args[i]+" ";
					}
					player.connection.disconnect(new TextComponentString(kickString.trim().isEmpty()?"Kicked using discord":kickString.trim()));
					discord.sendMessage("Kicked "+player.getName());
					return;
				}
			}discord.sendMessage(parsePlayerNotFoundMsg(args[0]));
		}
	}
	@Override
	public String getCommandUsage() {
		return super.getCommandUsage()+" <player> [message]";
	}

}
