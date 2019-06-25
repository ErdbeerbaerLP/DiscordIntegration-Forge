package de.erdbeerbaerlp.dcintegration;

/**
 * Class containing all config entries
 */
@Config(modid = DiscordIntegration.MODID, name = "Discord-Integration")
public class OldConfiguration {
	@Name("General Config")
	@Comment("General bot Configuration")
	public static category_general GENERAL = new category_general();
	@Name("Webhook")
	@Comment("Webhook configuration")
	public static category_webhook WEBHOOK = new category_webhook();
	@Name("Messages")
	@Comment("Customize messages of this mod")
	public static category_messages MESSAGES = new category_messages();
	@Comment("Configuration for built-in discord commands")
	public static category_commands COMMANDS = new category_commands();
	@Comment("Configurate the /discord command useable ingame")
	public static Configuration.discord_command DISCORD_COMMAND = new Configuration.discord_command();
	@Name("FTB Utilities")
	@Comment("Theese config values will only be used when FTB Utilities is installed!")
	public static category_ftbutilities FTB_UTILITIES = new category_ftbutilities();

	public static class category_general{

		@Comment({
			"Insert your Bot Token here!",
			"DO NOT SHARE IT WITH ANYONE!"
		})
		public String BOT_TOKEN = "INSERT TOKEN HERE!";
		public Discord.GameTypes BOT_GAME_TYPE = Discord.GameTypes.PLAYING;
		@Comment("The Name of the Game")
		public String BOT_GAME_NAME = "Minecraft";
		@Comment("The channel ID where the bot will be working in")
		public String CHANNEL_ID = "000000000";
		@Comment("Wether or not the Bot should modify the channel description")
		public boolean MODIFY_CHANNEL_DESCRIPTRION = true;
		@Comment("If you think the update check is annoying disable this")
		public boolean UPDATE_CHECK = true;
	}
	public static class category_webhook{

		@Comment("Wether or not the bot should use a webhook (it will create one)")
		public boolean BOT_WEBHOOK = false;
		@Comment("The avatar to be used for server messages")
		public String SERVER_AVATAR = "https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/master/images/srv.png";
		@Comment("The username of the server")
		public String SERVER_NAME = "Server";
	}

	public static class category_messages {
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
		@Comment({"The message to print to discord when it was possible to detect a server crash", "Will also be used in the channel description"})
		public String SERVER_CRASHED_MSG = "Server Crash Detected :thinking:";
		@Comment({"This is what will be displayed ingame when someone types into the bot\u00B4s channel", "PLACEHOLDERS:", "%name% - The username", "%id% - The user ID", "%msg% - The Message"})
		public String INGAME_DISCORD_MSG = "\u00A76[\u00A75DISCORD\u00A76]\u00A7r <%user%> %msg%";
		@Comment({"Supports MulitLined messages using \\n", "PLACEHOLDERS:", "%player% - The player\u00B4s name", "%name% - The advancement name", "%desc% - The advancement description"})
		public String PLAYER_ADVANCEMENT_MSG = "%player% just gained the advancement **%name%**\\n_%desc%_";
		@Comment({"Chat message when webhook is disabled", "PLACEHOLDERS:", "%player% - The player\u00B4s name", "%msg% - The chat message"})
		public String PLAYER_CHAT_MSG = "%player%: %msg%";
		@Comment({"Channel description while the server is online", "PLACEHOLDERS:", "%online% - Online player amount", "%max% - Maximum player count", "%tps% - Server TPS", "%motd% - The server MOTD (from server.properties!)", "%uptime% - The uptime of the server"})
		public String CHANNEL_DESCRIPTION = "%motd% (%online%/%max%) | %tps% TPS | Uptime: %uptime%";
		@Comment("Channel description while the server is offline")
		public String CHANNEL_DESCRIPTION_OFFLINE = "Server is Offline!";
		@Comment("Channel description while the server is starting")
		public String CHANNEL_DESCRIPTION_STARTING = "Starting...";
		@Comment({"PLACEHOLDERS:", "%player% - The player\u00B4s name"})
		public String PLAYER_TIMEOUT_MSG = "%player% timed out!";
		

	}
	public static class category_ftbutilities{
		@Comment("Print afk messages in discord")
		public boolean DISCORD_AFK_MSG_ENABLED = true;
		@Comment({"Format of the AFK message", "PLACEHOLDERS:", "%player% - The player\u00B4s name"})
		public String DISCORD_AFK_MSG = "%player% is now AFK";
		@Comment({"Format of the no longer AFK message", "PLACEHOLDERS:", "%player% - The player\u00B4s name"})
		public String DISCORD_AFK_MSG_END = "%player% is no longer AFK";
		@Comment("URL of the FTB Avatar icon")
		public String FTB_AVATAR_ICON = "https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/master/images/ftb.png";
		@Comment({"Format of the shutdown message printed when the server will shutdown/restart in 30 and 10 seconds","PLACEHOLDERS:", "%seconds% - The seconds remaining till shutdown (30 or 10)"})
		public String SHUTDOWN_MSG = "Server stopping in %seconds%!";
	}

	public static class category_commands{
		@Comment("The Role ID of your Admin Role")
		public String ADMIN_ROLE_ID = "0";
		@Comment("The prefix of the commands like list")
		public String CMD_PREFIX = "/";
		@Comment("The message for 'list' when no player is online")
		public String MSG_LIST_EMPTY = "There is no player online...";
		@Comment("The message for 'list' when one is online")
		public String MSG_LIST_ONE = "There is 1 player online:";
		@Comment({"The header for 'list'", "PLACEHOLDERS:", "%amount% - The amount of players online"})
		public String MSG_LIST_HEADER = "There are %amount% players online:";
		@Comment("Message sent when user does not have permission to run a command")
		public String MSG_NO_PERMISSION = "You don\u00B4t have permission to execute this command!";
		@Comment({"Message sent when an invalid command was typed", "PLACEHOLDERS:", "%prefix% - Command prefix"})
		public String MSG_UNKNOWN_COMMAND = "Unknown command, try `%prefix%help` for a list of commands";
		@Comment("Message if a player provides less arguments than required")
		public String MSG_NOT_ENOUGH_ARGUMENTS = "Not enough arguments";
		@Comment("Message if a player provides too many arguments")
		public String MSG_TOO_MANY_ARGUMENTS = "Too many arguments";
		@Comment({"Message if a player provides too many arguments", "PLACEHOLDERS:", "%player% - The player\u00B4s name"})
		public String MSG_PLAYER_NOT_FOUND = "Can not find player \"%player%\"";
	}

	public static class discord_command {
		@Comment("Enable the /discord command?")
		public boolean enabled = true;
		@Comment("The message displayed when typing /discord in the server chat")
		public String MESSAGE = "Join our discord! http://discord.gg/myserver";
		@Comment("The message shown when hovering the /discord command message")
		public String HOVER = "Click to open the invite url";
		@Comment("The url to open when clicking the /discord command text")
		public String URL = "http://discord.gg/myserver";
	}


}
