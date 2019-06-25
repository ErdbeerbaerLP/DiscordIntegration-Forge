package de.erdbeerbaerlp.dcintegration;

import static de.erdbeerbaerlp.dcintegration.Configuration.GENERAL;
import static de.erdbeerbaerlp.dcintegration.Configuration.WEBHOOK;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;

import de.erdbeerbaerlp.dcintegration.commands.DiscordCommand;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.managers.ChannelManager;
import net.dv8tion.jda.core.requests.RequestFuture;
import net.dv8tion.jda.core.utils.PermissionUtil;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class Discord implements EventListener{
	private final JDA jda;
	public boolean isKilled = false;
	private final List<DiscordCommand> commands = new ArrayList<DiscordCommand>();

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
			MinecraftServer minecraftServer = ServerLifecycleHooks.getCurrentServer();
			return LongStream.of(minecraftServer.tickTimeArray).sum() / minecraftServer.tickTimeArray.length * 1.0E-6D;
		}

		private double getAverageTPS() {
			return Math.min(1000.0 / getAverageTickCount(), 20);
		}

		public void run() {
			try {
				while(true) {
					getChannelManager().setTopic(
							Configuration.MESSAGES.CHANNEL_DESCRIPTION
							.replace("%tps%", ""+Math.round(getAverageTPS()))
							.replace("%online%", ""+ServerLifecycleHooks.getCurrentServer().getOnlinePlayerNames().length)
							.replace("%max%", ""+ServerLifecycleHooks.getCurrentServer().getMaxPlayers())
							.replace("%motd%", ServerLifecycleHooks.getCurrentServer().getMOTD())
							.replace("%uptime%", DiscordIntegration.getUptime())
							).complete();
					sleep(500);
				}
			}catch (InterruptedException | RuntimeException e) {

			}
		}
	};
	/**
	 * This thread is used to detect auto shutdown status using ftb utilities
	 *//*
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
	 *//*
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
				for(EntityPlayerMP player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
					final FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(universe.getPlayer(player));
					if(timers.containsKey(player) && data.afkTime < timers.get(player).getKey() && timers.get(player).getValue()) sendMessage(Configuration.FTB_UTILITIES.DISCORD_AFK_MSG_END
							.replace("%player%", player.getName()), Configuration.FTB_UTILITIES.FTB_AVATAR_ICON, "FTB Utilities", null);
					//					System.out.println(player.getName()+": "+data.afkTime+ ". Afk Time:"+ Ticks.get(FTBUtilitiesConfig.afk.notification_timer).millis());
					timers.put(player, new SimpleEntry<Long, Boolean>(data.afkTime, (timers.containsKey(player)? timers.get(player).getValue():false)));
				}

				timers.keySet().forEach((p)->{
					if(!ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().contains(p)) {
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
*/
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
		System.out.println("Bot Ready");
		jda.addEventListener(this);
		if(!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)){
			System.err.println("ERROR! Bot does not have all permissions to work!");
			throw new PermissionException("Bot requires message read, message write, embed links and manage messages");
		}
		if(Configuration.GENERAL.MODIFY_CHANNEL_DESCRIPTRION)
			if(!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_CHANNEL)) {
				Configuration.GENERAL.MODIFY_CHANNEL_DESCRIPTRION = false;
				System.err.println("ERROR! Bot does not have permission to manage channel, disabling channel description");
			}
		if(Configuration.WEBHOOK.BOT_WEBHOOK)
			if(!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_WEBHOOKS)) {
				Configuration.WEBHOOK.BOT_WEBHOOK = false;
				System.err.println("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
			}
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
			return getChannel().sendMessage(msg).submit();
	}
	/**
	 * Sends a message as player
	 * @param player Player
	 * @param msg Message
	 */
	public void sendMessage(PlayerEntity player, String msg) {
		sendMessage(player.getName().getUnformattedComponentText(), player.getUniqueID().toString(), msg);
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
			final WebhookClient cli = getWebhook().newClient().build();
			cli.send(b.build());
			cli.close();
		} else 
			getChannel().sendMessage(
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
				final WebhookClient cli = getWebhook().newClient().build();
				cli.send(b.build());
				cli.close();
			}else {
				final WebhookMessageBuilder b = new WebhookMessageBuilder();
				b.setContent(msg);
				b.setUsername(playerName);
				b.setAvatarUrl("https://minotar.net/avatar/"+UUID);
				final WebhookClient cli = getWebhook().newClient().build();
				cli.send(b.build());
				cli.close();
			}
		} else 
			if(playerName == Configuration.WEBHOOK.SERVER_NAME && UUID == "0000000") {
				getChannel().sendMessage(msg).complete();
			}
			else {
				getChannel().sendMessage(
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
			if(getChannel().getId().equals(ev.getChannel().getId()) && !ev.isWebhookMessage() && !ev.getAuthor().getId().equals(jda.getSelfUser().getId())) {
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
					ServerLifecycleHooks.getCurrentServer().getPlayerList().sendMessage(
							new StringTextComponent(Configuration.MESSAGES.INGAME_DISCORD_MSG
									.replace("%user%", ev.getAuthor().getName())
									.replace("%id%", ev.getAuthor().getId())
									.replace("%msg%", ev.getMessage().getContentRaw())).setStyle(new Style().setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new StringTextComponent("Sent by discord user \""+ev.getAuthor().getAsTag()+"\"")))));
			}

		}
	}
	public ChannelManager getChannelManager(){
		return new ChannelManager(getChannel());
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
	 * @return an instance of the webhook or null
	 */
	@Nullable
	public Webhook getWebhook() {
		if(!Configuration.WEBHOOK.BOT_WEBHOOK) return null;
		if(!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_WEBHOOKS)) {
			Configuration.WEBHOOK.BOT_WEBHOOK = false;
			System.out.println("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
			return null;
		}
		for(Webhook web : getChannel().getWebhooks().complete()) {
			if(web.getName().equals("MC_DISCORD_INTEGRATION")) {
				return web;
			}
		};
		return getChannel().createWebhook("MC_DISCORD_INTEGRATION").complete();
	}
	/**
	 * Used to stop all discord integration threads in background
	 */
	void stopThreads() { 
//		if(ftbUtilitiesAFKDetectThread.isAlive()) ftbUtilitiesAFKDetectThread.interrupt();
		if(updateChannelDesc.isAlive()) updateChannelDesc.interrupt();
//		if(ftbUtilitiesShutdownDetectThread.isAlive()) ftbUtilitiesShutdownDetectThread.interrupt();
	}
	/**
	 * @return A list of all registered commands
	 */
	public List<DiscordCommand> getCommandList() {
		return this.commands;
	}
	/**
	 * 
	 * @return The admin role of the server
	 */
	public Role getAdminRole() {
		return (Configuration.COMMANDS.ADMIN_ROLE_ID.equals("0") || Configuration.COMMANDS.ADMIN_ROLE_ID.trim().isEmpty()) ? null : jda.getRoleById(Configuration.COMMANDS.ADMIN_ROLE_ID);
	}
	/**
	 * @return the specified text channel
	 */
	public TextChannel getChannel() {
		return jda.getTextChannelById(GENERAL.CHANNEL_ID);
	}
}

