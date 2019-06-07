package de.erdbeerbaerlp.dcintegration;

import java.net.URL;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;

@Config(modid = ModClass.MODID, name = "Discord-Integration")
public class Configuration {
	public static category_general GENERAL = new category_general();
	public static category_webhook WEBHOOK = new category_webhook();
	public static category_messages MESSAGES = new category_messages();
	
	static class category_general{

		@Comment({
			"Insert your Bot Token here!",
			"DO NOT SHARE IT WITH ANYONE!"
		})
		public String BOT_TOKEN = "INSERT TOKEN HERE!";
		//	@Comment("")
		public Discord.GameTypes BOT_GAME_TYPE = Discord.GameTypes.PLAYING;
		@Comment("The Name of the Game")
		public String BOT_GAME_NAME = "Minecraft";
		@Comment("The channel ID where the bot will be working in")
		public String CHANNEL_ID = "000000000";
		
	}
	static class category_webhook{

		@Comment("Wether or not the bot should use a webhook")
		public boolean BOT_WEBHOOK = false;
		@Comment("The avatar to be used for server messages")
		public String SERVER_AVATAR = "https://adnservers.com/templates/GigaTick/html/img/server-icon-1.png";
	}
	
	static class category_messages {
		@Comment("This message will edited in / sent when the server finished starting")
		public String SERVER_STARTED_MSG = "Server Started!";
		@Comment({"Message to show while the server is starting", "This will be edited to SERVER_STARTED_MSG when webhook is false"})
		public String SERVER_STARTING_MSG = "Server Starting...";
		@Comment("This message will be sent when the server was stopped")
		public String SERVER_STOPPED_MSG = "Server Stopped!";
		@Comment({"PLACEHOLDERS:", "%player% - The player\u00B4s name"})
		public String PLAYER_JOINED_MSG = "%player% joined";
		@Comment({"PLACEHOLDERS:", "%player% - The player\u00B4s name"})
		public String PLAYER_LEFT_MSG = "%player% left";
		@Comment({"PLACEHOLDERS:", "%player% - The player\u00B4s name", "%msg% - The death message"})
		public String PLAYER_DEATH_MSG = "%player% %msg%";
		@Comment("The message to print to discord when it was possible to detect a server crash")
		public String SERVER_CRASHED_MSG = "Server Crash Detected :thinking:";
		@Comment({"This is what will be displayed ingame when someone types into the bot\u00B4s channel", "PLACEHOLDERS:", "%name% - The username", "%id% - The user ID", "%msg% - The Message"})
		public String INGAME_DISCORD_MSG = "\u00A76[\u00A75DISCORD\u00A76]\u00A7r <%user%> %msg%";
		@Comment({"Supports MulitLined messages using \\n", "PLACEHOLDERS:", "%player% - The player\u00B4s name", "%name% - The advancement name", "%desc% - The advancement description"})
		public String PLAYER_ADVANCEMENT_MSG = "%player% just gained the advancement **%name%**\\n_%desc%_";
		@Comment({"Chat message when webhook is disabled", "PLACEHOLDERS:", "%player% - The player\u00B4s name", "%msg% - The chat message"})
		public String PLAYER_CHAT_MSG = "%player%: %msg%";
	}
	
}
