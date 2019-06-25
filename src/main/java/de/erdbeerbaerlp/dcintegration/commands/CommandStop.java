package de.erdbeerbaerlp.dcintegration.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class CommandStop extends DiscordCommand {

	@Override
	public String getName() {
		return "stop";
	}

	@Override
	public String[] getAliases() {
		return new String[0];
	}

	@Override
	public String getDescription() {
		return "Stops the server";
	}

	@Override
	public void execute(String[] args, MessageReceivedEvent cmdMsg) {
		discord.sendMessage("Stopping server");
		server.initiateShutdown(false);
	}
	@Override
	public boolean adminOnly() {
		return true;
	}
}
