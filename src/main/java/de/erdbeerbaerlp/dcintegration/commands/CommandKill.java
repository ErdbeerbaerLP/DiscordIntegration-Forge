package de.erdbeerbaerlp.dcintegration.commands;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import de.erdbeerbaerlp.dcintegration.Configuration;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.DamageSource;

public class CommandKill extends DiscordCommand {

	@Override
	public String getName() {
		return "kill";
	}

	@Override
	public String[] getAliases() {
		return new String[0];
	}

	@Override
	public String getDescription() {
		return "Kills a player";
	}

	@Override
	public void execute(String[] args, MessageReceivedEvent cmdMsg) {
		if(args.length <= 0 || args[0].isEmpty()) discord.sendMessage(Configuration.INSTANCE.msgNotEnoughArgs.get());
		else if(args.length > 1) discord.sendMessage(Configuration.INSTANCE.msgTooManyArgs.get());
		else {
			for(ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
				if(player.getName().getUnformattedComponentText().equalsIgnoreCase(args[0])) {
					player.attackEntityFrom(DamageSource.causePlayerDamage(new ServerPlayerEntity(server, player.getServerWorld(), new GameProfile(UUID.fromString(cmdMsg.getAuthor().getDiscriminator()+"0000-DC00-0000-"+(cmdMsg.getMessage().getAuthor().getIdLong()+"").substring(0, 3)+"-"+(cmdMsg.getMessage().getAuthor().getIdLong()+"").substring(4)), cmdMsg.getAuthor().getName()), new PlayerInteractionManager(player.getServerWorld()))).setDamageBypassesArmor().setDamageAllowedInCreativeMode(), Float.MAX_VALUE);
					discord.sendMessage("Killed "+player.getName());
					return;
				}
			}
			 discord.sendMessage(parsePlayerNotFoundMsg(args[0]));
		}
	}
	@Override
	public String getCommandUsage() {
		return super.getCommandUsage()+" <player>";
	}
	@Override
	public boolean adminOnly() {
		return true;
	}
	

}
