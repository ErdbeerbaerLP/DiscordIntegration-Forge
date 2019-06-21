package de.erdbeerbaerlp.dcintegration;

import static de.erdbeerbaerlp.dcintegration.Configuration.GENERAL;
import static de.erdbeerbaerlp.dcintegration.Configuration.WEBHOOK;
import static de.erdbeerbaerlp.dcintegration.DiscordIntegration.started;

import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;

import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.math.Ticks;
import com.feed_the_beast.ftbutilities.FTBUtilitiesConfig;
import com.feed_the_beast.ftbutilities.data.FTBUtilitiesPlayerData;
import com.feed_the_beast.ftbutilities.data.FTBUtilitiesUniverseData;

import de.erdbeerbaerlp.dcintegration.commands.DiscordCommand;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.managers.ChannelManager;
import net.dv8tion.jda.core.requests.RequestFuture;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class Discord implements EventListener{
	private final JDA jda;
	private Webhook w = null;
	private final TextChannel channel;
	public final ChannelManager channelManager;
	public boolean isKilled = false;
	private final List<DiscordCommand> commands = new ArrayList<DiscordCommand>();
	private final Role adminRole;
	
	public enum GameTypes{
		WATCHING,PLAYING,LISTENING,DISABLED;
	}
	/**
	 * This thread is used to update the channel description
	 */
	Thread updateChannelDesc = new Thread() {
		{
			this.setName("[DC INTEGRATION] Channel Description Updater");
			this.setDaemon(false);
			this.setPriority(MAX_PRIORITY);
		}
		private double getAverageTickCount() {
			MinecraftServer minecraftServer = FMLCommonHandler.instance().getMinecraftServerInstance();
			return LongStream.of(minecraftServer.tickTimeArray).sum() / minecraftServer.tickTimeArray.length * 1.0E-6D;
		}

		private double getAverageTPS() {
			return Math.min(1000.0 / getAverageTickCount(), 20);
		}
		private String getUptime() {
	        if (started == 0) {
	            return "?????";
	        }

	        long diff = new Date().getTime() - started;

	        int seconds = (int) Math.floorDiv(diff, 1000);
	        if (seconds < 60) {
	            return seconds + " second" + (seconds == 1 ? "" : "s");
	        }
	        int minutes = Math.floorDiv(seconds, 60);
	        seconds -= minutes * 60;
	        if (minutes < 60) {
	            return minutes + " minute" + (minutes == 1 ? "" : "s") + ", " + seconds + " second" + (seconds == 1 ? "" : "s");
	        }
	        int hours = Math.floorDiv(minutes, 60);
	        minutes -= hours * 60;
	        if (hours < 24) {
	            return hours + " hour" + (hours == 1 ? "" : "s") + ", " + minutes + " minute" + (minutes == 1 ? "" : "s") + ", " + seconds + " second" + (seconds == 1 ? "" : "s");
	        }
	        int days = Math.floorDiv(hours, 24);
	        hours -= days * 24;
	        return days + " day" + (days == 1 ? "" : "s") + ", " + hours + " hour" + (hours == 1 ? "" : "s") + ", " + minutes + " minute" + (minutes == 1 ? "" : "s") + ", " + seconds + " second" + (seconds == 1 ? "" : "s");
	    }
		public void run() {
			try {
				while(true) {
					channelManager.setTopic(
							Configuration.MESSAGES.CHANNEL_DESCRIPTION
							.replace("%tps%", ""+Math.round(getAverageTPS()))
							.replace("%online%", ""+FMLCommonHandler.instance().getMinecraftServerInstance().getOnlinePlayerProfiles().length)
							.replace("%max%", ""+FMLCommonHandler.instance().getMinecraftServerInstance().getMaxPlayers())
							.replace("%motd%", FMLCommonHandler.instance().getMinecraftServerInstance().getMOTD())
							.replace("%uptime%", getUptime())
							).complete();
					sleep(500);
				}
			}catch (InterruptedException | RuntimeException e) {

			}
		}
	};
	/**
	 * This thread is used to detect auto shutdown status using ftb utilities
	 */
	Thread ftbUtilitiesShutdownDetectThread = new Thread() {
		{
			setName("[DC INTEGRATION] FTB Utilities shutdown detector");
			setDaemon(true);
			setPriority(MAX_PRIORITY);
		}
		public void run() {
			while(true) {
				final long timeLeft = TimeUnit.MILLISECONDS.toSeconds(FTBUtilitiesUniverseData.shutdownTime-Instant.now().toEpochMilli());

				if(timeLeft > 30);
				else if(timeLeft == 30) sendMessage(Configuration.FTB_UTILITIES.SHUTDOWN_MSG.replace("%seconds%", "30"), Configuration.FTB_UTILITIES.FTB_AVATAR_ICON, "FTB Utilities", null);
				else if(timeLeft == 10) {
					sendMessage(Configuration.FTB_UTILITIES.SHUTDOWN_MSG.replace("%seconds%", "10"), Configuration.FTB_UTILITIES.FTB_AVATAR_ICON, "FTB Utilities", null);
					break;
				}

				try {
					sleep(TimeUnit.SECONDS.toMillis(1));
				} catch (InterruptedException e) {}
			} interrupt();
		}
	};
	/**
	 * This thread is used to detect AFK states using ftb utilities
	 */
	Thread ftbUtilitiesAFKDetectThread = new Thread() {
		{
			setName("[DC INTEGRATION] FTB Utilities AFK detector");
			setDaemon(true);
			setPriority(MAX_PRIORITY);
		}
		public void run() {
			final Map<EntityPlayerMP, Entry<Long, Boolean>> timers = new HashMap<EntityPlayerMP, Entry<Long, Boolean>>();
			final Universe universe = Universe.get();
			while(true) {
				for(EntityPlayerMP player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
					final FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(universe.getPlayer(player));
					if(timers.containsKey(player) && data.afkTime < timers.get(player).getKey() && timers.get(player).getValue()) sendMessage(Configuration.FTB_UTILITIES.DISCORD_AFK_MSG_END
							.replace("%player%", player.getName()), Configuration.FTB_UTILITIES.FTB_AVATAR_ICON, "FTB Utilities", null);
					//					System.out.println(player.getName()+": "+data.afkTime+ ". Afk Time:"+ Ticks.get(FTBUtilitiesConfig.afk.notification_timer).millis());
					timers.put(player, new SimpleEntry<Long, Boolean>(data.afkTime, (timers.containsKey(player)? timers.get(player).getValue():false)));
				}

				timers.keySet().forEach((p)->{
					if(!FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers().contains(p)) {
						timers.remove(p); //Clean up
					}else {
						final boolean afk = timers.get(p).getKey() >= Ticks.get(FTBUtilitiesConfig.afk.notification_timer).millis();
						if(afk && !timers.get(p).getValue()) sendMessage(Configuration.FTB_UTILITIES.DISCORD_AFK_MSG
								.replace("%player%", p.getName()), Configuration.FTB_UTILITIES.FTB_AVATAR_ICON, "FTB Utilities", null);
						timers.put(p, new SimpleEntry<Long, Boolean>(timers.get(p).getKey(), afk));

					}
				});
				try {
					sleep(900);
				} catch (InterruptedException e) {}
			}
		}
	};
	
	/**
	 * Constructor for this class
	 */
	Discord() throws LoginException, InterruptedException {
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
		this.channelManager = new ChannelManager(channel);
		System.out.println("Bot Ready");
		for(Webhook web : channel.getWebhooks().complete()) {
			if(web.getName().equals("MC_DISCORD_INTEGRATION")) {
				w = web;
				break;
			}
		};
		if(w == null) w = channel.createWebhook("MC_DISCORD_INTEGRATION").complete();
		jda.addEventListener(this);
		this.adminRole = (Configuration.COMMANDS.ADMIN_ROLE_ID.equals("0")) ? null : jda.getRoleById(Configuration.COMMANDS.ADMIN_ROLE_ID);
	}
	/**
	 * Sends a message when *not* using a webhook and returns it as RequestFuture<Message> or null when using a webhook
	 * @param msg message 
	 * @return Sent message
	 */
	public RequestFuture<Message> sendMessageReturns(String msg) {
		if(WEBHOOK.BOT_WEBHOOK) 
			return null;
		else
			return channel.sendMessage(msg).submit();
	}
	/**
	 * Sends a message as player
	 * @param player Player
	 * @param msg Message
	 */
	public void sendMessage(EntityPlayerMP player, String msg) {
		sendMessage(player.getName(), player.getUniqueID().toString(), msg);
	}
	/**
	 * Sends a message as server
	 * @param msg Message
	 */
	public void sendMessage(String msg) {
		sendMessage(Configuration.WEBHOOK.SERVER_NAME, "0000000", msg);
	}
	/**
	 * Sends a message to discord with custom avatar url (when using a webhook)
	 * @param msg Message
	 * @param avatarURL URL of the avatar image
	 * @param name Name of the fake player
	 * @param nullMe Just pass null, required to differ from a different method
	 */
	public void sendMessage(String msg, String avatarURL, String name, @Nullable Void nullMe) {
		if(isKilled) return;
		if(WEBHOOK.BOT_WEBHOOK) {
			final WebhookMessageBuilder b = new WebhookMessageBuilder();
			b.setContent(msg);
			b.setUsername(name);
			b.setAvatarUrl(avatarURL);
			final WebhookClient cli = w.newClient().build();
			cli.send(b.build());
			cli.close();
		} else 
			channel.sendMessage(
					Configuration.MESSAGES.PLAYER_CHAT_MSG
					.replace("%player%", name)
					.replace("%msg%", msg)
					).complete();
	}
	/**
	 * Sends a message to discord
	 * @param playerName the name of the player
	 * @param UUID the player uuid
	 * @param msg the message to send
	 */
	public void sendMessage(String playerName, String UUID, String msg) {
		if(isKilled) return;
		if(WEBHOOK.BOT_WEBHOOK) {
			if(playerName == Configuration.WEBHOOK.SERVER_NAME && UUID == "0000000") {
				final WebhookMessageBuilder b = new WebhookMessageBuilder();
				b.setContent(msg);
				b.setUsername(Configuration.WEBHOOK.SERVER_NAME);
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
			if(playerName == Configuration.WEBHOOK.SERVER_NAME && UUID == "0000000") {
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
	/**
	 * Kills the discord bot
	 */
	void kill() {
		stopThreads();
		this.isKilled = true;
		jda.shutdown();
	}
	/**
	 * Event handler to handle messages
	 */
	@Override
	public void onEvent(Event event) {
		if(isKilled) return;
		if(event instanceof MessageReceivedEvent) {
			final MessageReceivedEvent ev = (MessageReceivedEvent) event;
			if(channel.getId().equals(ev.getChannel().getId()) && !ev.isWebhookMessage() && !ev.getAuthor().getId().equals(jda.getSelfUser().getId())) {
				if(ev.getMessage().getContentRaw().startsWith(Configuration.COMMANDS.CMD_PREFIX)) {
					final String[] command = ev.getMessage().getContentRaw().replaceFirst(Configuration.COMMANDS.CMD_PREFIX, "").split(" ");
					String argumentsRaw = "";
					for(int i=1;i<command.length;i++) {
						argumentsRaw = argumentsRaw+command[i]+" ";
					}
					argumentsRaw = argumentsRaw.trim();
					boolean hasPermission = true;
					boolean executed = false;
					for(DiscordCommand cmd : commands) {

						if(cmd.getName().equals(command[0])){
							if(cmd.canUserExecuteCommand(ev.getAuthor())) {
								cmd.execute(argumentsRaw.split(" "), ev);
								executed = true;
							}else hasPermission = false;

						}

						for(String alias : cmd.getAliases()) {
							if(alias.equals(command[0])) {
								if(cmd.canUserExecuteCommand(ev.getAuthor())) {
									cmd.execute(argumentsRaw.split(" "), ev);
									executed = true;
								}else hasPermission = false;
							}
						}
					}
					if(!hasPermission) {
						sendMessage(Configuration.COMMANDS.MSG_NO_PERMISSION);
						return;
					}
					if(!executed){
						sendMessage(Configuration.COMMANDS.MSG_UNKNOWN_COMMAND.replace("%prefix%", Configuration.COMMANDS.CMD_PREFIX));
						return;
					}

				}else
					FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(
							new TextComponentString(Configuration.MESSAGES.INGAME_DISCORD_MSG
									.replace("%user%", ev.getAuthor().getName())
									.replace("%id%", ev.getAuthor().getId())
									.replace("%msg%", ev.getMessage().getContentRaw())).setStyle(new Style().setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new TextComponentString("Sent by discord user \""+ev.getAuthor().getAsTag()+"\"")))));
			}

		}
	}
	/**
	 * Registers an {@link DiscordCommand}
	 * @param cmd command
	 * @return if the registration was successful
	 */
	public boolean registerCommand(DiscordCommand cmd) {
		for(DiscordCommand c : commands) {
			if(cmd.equals(c)) return false;
		}
		return commands.add(cmd);
	}
	/**
	 * 
	 * @return an instance of the used TextChannel
	 */
	public TextChannel getChannel() {
		return channel;
	}
	/**
	 * 
	 * @return an instance of the webhook or null
	 */
	@Nullable
	public Webhook getWebhook() {
		return w;
	}
	/**
	 * Used to stop all discord integration threads in background
	 */
	void stopThreads() { 
		if(ftbUtilitiesAFKDetectThread.isAlive()) ftbUtilitiesAFKDetectThread.interrupt();
		if(updateChannelDesc.isAlive()) updateChannelDesc.interrupt();
		if(ftbUtilitiesShutdownDetectThread.isAlive()) ftbUtilitiesShutdownDetectThread.interrupt();
	}
	/**
	 * 
	 * @return A list of all commands
	 */
	public List<DiscordCommand> getCommandList() {
		return this.commands;
	}
	/**
	 * 
	 * @return The admin role of the server
	 */
	public Role getAdminRole() {
		return this.adminRole;
	}
}
