package de.erdbeerbaerlp.dcintegration;

import java.util.concurrent.ExecutionException;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.requests.RequestFuture;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

@Mod(modid = ModClass.MODID, version = ModClass.VERSION, name = ModClass.NAME, serverSideOnly = true, acceptableRemoteVersions = "*")
public class ModClass {
	public static final String NAME = "Discord Chat Integration";
	public static final String VERSION = "0.0.0";
	public static final String MODID = "dcintegration";
	public static Discord discord;
	private RequestFuture<Message> startingMsg;
	public ModClass() {

	}
	@EventHandler
	public void preInit(FMLPreInitializationEvent ev) {
		System.out.println("Loading mod");
		try {
			discord = new Discord();

		} catch (Exception e) {
			System.err.println("Failed to login: "+e.getMessage());
			discord = null;
		}
		MinecraftForge.EVENT_BUS.register(this);
	}
	@EventHandler
	public void init(FMLInitializationEvent ev) {
		if(discord != null && !Configuration.WEBHOOK.BOT_WEBHOOK) this.startingMsg = discord.sendMessageReturns("Server Starting...");
		if(discord != null && Configuration.GENERAL.MODIFY_CHANNEL_DESCRIPTRION) discord.channelManager.setTopic(Configuration.MESSAGES.CHANNEL_DESCRIPTION_STARTING).complete();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if(discord != null && Configuration.GENERAL.MODIFY_CHANNEL_DESCRIPTRION) discord.channelManager.setTopic(Configuration.MESSAGES.SERVER_CRASHED_MSG).complete();
				if(discord != null && !discord.isKilled) discord.kill(Configuration.MESSAGES.SERVER_CRASHED_MSG);
			}
		});
	}
	@EventHandler
	public void serverAboutToStart(FMLServerAboutToStartEvent ev) {

	}
	@EventHandler
	public void serverStarting(FMLServerStartingEvent ev) {

	}
	@EventHandler
	public void serverStarted(FMLServerStartedEvent ev) {
		if(discord != null) if(startingMsg != null) try {
			this.startingMsg.get().editMessage(Configuration.MESSAGES.SERVER_STARTED_MSG).queue();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		else discord.sendMessage(Configuration.MESSAGES.SERVER_STARTED_MSG);
		if(discord != null && Configuration.GENERAL.MODIFY_CHANNEL_DESCRIPTRION) discord.updateChannelDesc.start();
	}
	private boolean stopped = false;
	@EventHandler
	public void serverStopping(FMLServerStoppingEvent ev) {
		stopped = true;
	}
	@EventHandler
	public void serverStopped(FMLServerStoppedEvent ev) {
		if(stopped)
			if(discord != null) discord.kill(Configuration.MESSAGES.SERVER_STOPPED_MSG);
	}
	@SubscribeEvent
	public void playerJoin(PlayerLoggedInEvent ev) {
		if(discord != null) discord.sendMessage(
				Configuration.MESSAGES.PLAYER_JOINED_MSG
				.replace("%player%", ev.player.getName())
				);
	}
	public static EntityPlayerMP lastTimeout;
	@SubscribeEvent
	public void playerLeave(PlayerLoggedOutEvent ev) {

		if(discord != null && !ev.player.equals(lastTimeout))
			discord.sendMessage(
					Configuration.MESSAGES.PLAYER_LEFT_MSG
					.replace("%player%", ev.player.getName())
					);
		else if(discord != null && ev.player.equals(lastTimeout)) {
			if(discord != null && lastTimeout == null)
				discord.sendMessage(
						Configuration.MESSAGES.PLAYER_TIMEOUT_MSG
						.replace("%player%", ev.player.getName())
						);
			lastTimeout = null;
		}
	}

	@SubscribeEvent
	public void chat(ServerChatEvent ev) {
		if(discord != null) discord.sendMessage(ev.getPlayer(), ev.getMessage());
	}
	@SubscribeEvent
	public void death(LivingDeathEvent ev) {
		if(ev.getEntity() instanceof EntityPlayerMP) {
			if(discord!=null) discord.sendMessage(
					Configuration.MESSAGES.PLAYER_DEATH_MSG
					.replace("%player%", ((EntityPlayerMP) ev.getEntity()).getName())
					.replace("%msg%", ev.getSource().getDeathMessage(ev.getEntityLiving()).getUnformattedText().replace(ev.getEntity().getName(), ""))
					);
		}
	}
	@SubscribeEvent
	public void advancement(AdvancementEvent ev) {
		if(discord != null) discord.sendMessage(
				Configuration.MESSAGES.PLAYER_ADVANCEMENT_MSG
				.replace("%player%", ev.getEntityPlayer().getName())
				.replace("%name%", ev.getAdvancement().getDisplay().getTitle().getUnformattedText())
				.replace("%desc%", ev.getAdvancement().getDisplay().getDescription().getUnformattedText())
				.replace("\\n", "\n")
				);
	}
}
