package de.erdbeerbaerlp.dcintegration;

import static de.erdbeerbaerlp.dcintegration.Configuration.GENERAL;
import static de.erdbeerbaerlp.dcintegration.Configuration.WEBHOOK;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.requests.RequestFuture;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class Discord implements EventListener{
	private final JDA jda;
	private Webhook w = null;
	private final TextChannel channel;
	public boolean isKilled = false;
	public enum GameTypes{
		WATCHING,PLAYING,LISTENING,DISABLED;
	}


	public Discord() throws LoginException, InterruptedException {
		final JDABuilder b = new JDABuilder(GENERAL.BOT_TOKEN);
		switch (GENERAL.BOT_GAME_TYPE) {
		case DISABLED:
			break;
		case LISTENING:
			b.setGame(Game.listening(GENERAL.BOT_GAME_NAME));
			break;
		case PLAYING:
			b.setGame(Game.playing(GENERAL.BOT_GAME_NAME));
			break;
		case WATCHING:
			b.setGame(Game.watching(GENERAL.BOT_GAME_NAME));
			break;
		}
		this.jda = b.build().awaitReady();
		this.channel = jda.getTextChannelById(GENERAL.CHANNEL_ID);
		System.out.println("Bot Ready");
		for(Webhook web : channel.getWebhooks().complete()) {
			if(web.getName().equals("MC_DISCORD_INTEGRATION")) w = web;
		};
		if(w == null) w = channel.createWebhook("MC_DISCORD_INTEGRATION").complete();
		jda.addEventListener(this);
	}






	@Nullable
	public RequestFuture<Message> sendMessageReturns(String msg) {
		if(WEBHOOK.BOT_WEBHOOK) 
			return null;
		else
			return channel.sendMessage(msg).submit();
	}
	public void sendMessage(EntityPlayerMP player, String msg) {
		sendMessage(player.getName(), player.getUniqueID().toString(), msg);
	}
	public void sendMessage(String msg) {
		sendMessage("SERVER", "0000000", msg);
	}
	public void sendMessage(String playerName, String UUID, String msg) {
		if(isKilled) return;
		if(WEBHOOK.BOT_WEBHOOK) {
			if(playerName == "SERVER" && UUID == "0000000") {
				final WebhookMessageBuilder b = new WebhookMessageBuilder();
				b.setContent(msg);
				b.setUsername("Server");
				b.setAvatarUrl(Configuration.WEBHOOK.SERVER_AVATAR);
				final WebhookClient cli = w.newClient().build();
				cli.send(b.build());
				cli.close();
			}else {
				final WebhookMessageBuilder b = new WebhookMessageBuilder();
				b.setContent(msg);
				b.setUsername(playerName);
				b.setAvatarUrl("https://minotar.net/avatar/"+UUID);
				final WebhookClient cli = w.newClient().build();
				cli.send(b.build());
				cli.close();
			}
		} else 
			if(playerName == "SERVER" && UUID == "0000000") {
				channel.sendMessage(msg).complete();
			}
			else {
				channel.sendMessage(
						Configuration.MESSAGES.PLAYER_CHAT_MSG
						.replace("%player%", playerName)
						.replace("%msg%", msg)
						).complete();
			}
	}

	public void kill() {
		jda.shutdownNow();
		this.isKilled = true;
	}

	public void kill(String msg) {
		if(!msg.isEmpty()) sendMessage(msg);
		kill();
	}

	@Override
	public void onEvent(Event event) {
		if(isKilled) return;
		if(event instanceof MessageReceivedEvent) {
			final MessageReceivedEvent ev = (MessageReceivedEvent) event;
			if(channel.getId().equals(ev.getChannel().getId()) && !ev.isWebhookMessage() && !ev.getAuthor().getId().equals(jda.getSelfUser().getId()))
				FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(
						new TextComponentString(Configuration.MESSAGES.INGAME_DISCORD_MSG
								.replace("%user%", ev.getAuthor().getName())
								.replace("%id%", ev.getAuthor().getId())
								.replace("%msg%", ev.getMessage().getContentRaw())));
		}
	}
}
