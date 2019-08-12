package de.erdbeerbaerlp.dcintegration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Configuration {
    public static final ForgeConfigSpec cfgSpec;
    public static final Configuration INSTANCE;

    static {
        {
            final Pair<Configuration, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Configuration::new);
            INSTANCE = specPair.getLeft();
            cfgSpec = specPair.getRight();
        }
    }

    //#########################
    //#        GENERAL        #
    //#########################
    public final ForgeConfigSpec.ConfigValue<String> botToken;
    public final ForgeConfigSpec.ConfigValue<String> botPresenceName;
    public final ForgeConfigSpec.EnumValue<Discord.GameTypes> botPresenceType;
    public final ForgeConfigSpec.ConfigValue<String> botChannel;
    public final ForgeConfigSpec.BooleanValue botModifyDescription;
    public final ForgeConfigSpec.BooleanValue updateCheck;
    //#########################
    //#        WEBHOOK        #
    //#########################
    public final ForgeConfigSpec.BooleanValue enableWebhook;
    public final ConfigValue<String> serverAvatar;
    public final ForgeConfigSpec.ConfigValue<String> serverName;
    //#########################
    //#       MESSAGES        #
    //#########################
    public final ForgeConfigSpec.ConfigValue<String> msgServerStarted;
    public final ForgeConfigSpec.ConfigValue<String> msgServerStarting;
    public final ForgeConfigSpec.ConfigValue<String> msgServerStopped;
    public final ForgeConfigSpec.ConfigValue<String> msgPlayerJoin;
    public final ForgeConfigSpec.ConfigValue<String> msgPlayerLeave;
    public final ForgeConfigSpec.ConfigValue<String> msgPlayerDeath;
    public final ForgeConfigSpec.ConfigValue<String> msgServerCrash;
    public final ForgeConfigSpec.ConfigValue<String> ingameDiscordMsg;
    public final ForgeConfigSpec.ConfigValue<String> msgAdvancement;
    public final ForgeConfigSpec.ConfigValue<String> msgChatMessage;
    public final ForgeConfigSpec.ConfigValue<String> description;
    public final ForgeConfigSpec.ConfigValue<String> descriptionOffline;
    public final ForgeConfigSpec.ConfigValue<String> descriptionStarting;
    public final ForgeConfigSpec.ConfigValue<String> msgPlayerTimeout;
    public final ForgeConfigSpec.BooleanValue sayOutput;

    //#########################
    //#       COMMANDS        #
    //#########################
    public final ForgeConfigSpec.ConfigValue<String> adminRoleId;
    public final ForgeConfigSpec.ConfigValue<String> prefix;
    public final ForgeConfigSpec.ConfigValue<String> msgListEmpty;
    public final ForgeConfigSpec.ConfigValue<String> msgListOne;
    public final ForgeConfigSpec.ConfigValue<String> msgListHeader;
    public final ForgeConfigSpec.ConfigValue<String> msgNoPermission;
    public final ForgeConfigSpec.ConfigValue<String> msgUnknownCommand;
    public final ForgeConfigSpec.ConfigValue<String> msgNotEnoughArgs;
    public final ForgeConfigSpec.ConfigValue<String> msgTooManyArgs;
    public final ForgeConfigSpec.ConfigValue<String> msgPlayerNotFound;
    public final ForgeConfigSpec.ConfigValue<String> jsonCommands;
    public final ForgeConfigSpec.ConfigValue<String> senderUUID;

    //#########################
    //#    INGAME-COMMAND     #
    //#########################
    public final ForgeConfigSpec.BooleanValue dcCmdEnabled;
    public final ForgeConfigSpec.ConfigValue<String> dcCmdMsg;
    public final ForgeConfigSpec.ConfigValue<String> dcCmdMsgHover;
    public final ForgeConfigSpec.ConfigValue<String> dcCmdURL;

    //	//#########################
//	//#     FTB-UTILITIES     # XXX Not implemented, since FTB Utilities is still on 1.12
//	//#########################
//	public final ForgeConfigSpec.BooleanValue ftbuAfkMsgEnabled;
//	public final ForgeConfigSpec.ConfigValue<String> ftbuAFKMsg;
//	public final ForgeConfigSpec.ConfigValue<String> ftbuAFKMsgEnd;
//	public final ForgeConfigSpec.ConfigValue<String> ftbuAvatar;
//	public final ForgeConfigSpec.ConfigValue<String> ftbuShutdownMsg;
    Configuration(final ForgeConfigSpec.Builder builder) {
        //#########################
        //#        GENERAL        #
        //#########################
        builder.comment("General bot Configuration").push("generalSettings");
        botToken = builder
                .comment("Insert your Bot Token here!", "DO NOT SHARE IT WITH ANYONE!")
                .define("botToken", "INSERT BOT TOKEN HERE");
        botPresenceName = builder
                .comment("The Name of the Game")
                .define("botPresenceName", "Minecraft");
        botPresenceType = builder
                .defineEnum("botPresenceType", Discord.GameTypes.PLAYING);
        botChannel = builder
                .comment("The channel ID where the bot will be working in")
                .define("botChannel", "000000000");
        botModifyDescription = builder
                .comment("Wether or not the Bot should modify the channel description")
                .define("botModifyDescription", true);
        updateCheck = builder
                .comment("If you think the update check is annoying disable this", "Update checking is not yet implemented!!!")
                .define("updateCheck", true);
        builder.pop();
        //#########################
        //#        WEBHOOK        #
        //#########################
        builder.comment("Webhook configuration").push("webhook");
        enableWebhook = builder
                .comment("Wether or not the bot should use a webhook (it will create one)")
                .define("enableWebhook", false);
        serverAvatar = builder
                .comment("The avatar to be used for server messages")
                .define("serverAvatar", "https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/master/images/srv.png");
        serverName = builder
                .comment("Wether or not the bot should use a webhook (it will create one)")
                .define("serverName", "Server");
        builder.pop();
        //#########################
        //#       MESSAGES        #
        //#########################
        builder.comment("Customize messages of this mod").push("messages");
        msgServerStarted = builder
                .comment("This message will edited in / sent when the server finished starting")
                .define("msgServerStarted", "Server Started!");
        msgServerStarting = builder
                .comment("Message to show while the server is starting", "This will be edited to SERVER_STARTED_MSG when webhook is false")
                .define("msgServerStarting", "Server Starting...");
        msgServerStopped = builder
                .comment("This message will be sent when the server was stopped")
                .define("msgServerStopped", "Server Stopped!");
        msgPlayerJoin = builder
                .comment("PLACEHOLDERS:", "%player% - The player\u00B4s name")
                .define("msgPlayerJoin", "%player% joined");
        msgPlayerLeave = builder
                .comment("PLACEHOLDERS:", "%player% - The player\u00B4s name")
                .define("msgPlayerLeave", "%player% left");
        msgPlayerDeath = builder
                .comment("PLACEHOLDERS:", "%player% - The player\u00B4s name", "%msg% - The death message")
                .define("msgPlayerDeath", "%player% %msg%");
        msgServerCrash = builder
                .comment("The message to print to discord when it was possible to detect a server crash", "Will also be used in the channel description")
                .define("msgServerCrash", "Server Crash Detected :thinking:");
        ingameDiscordMsg = builder
                .comment("This is what will be displayed ingame when someone types into the bot\u00B4s channel", "PLACEHOLDERS:", "%name% - The username", "%id% - The user ID", "%msg% - The Message")
                .define("ingameDiscordMsg", "\u00A76[\u00A75DISCORD\u00A76]\u00A7r <%user%> %msg%");
        msgAdvancement = builder
                .comment("Supports MulitLined messages using \\n", "PLACEHOLDERS:", "%player% - The player\u00B4s name", "%name% - The advancement name", "%desc% - The advancement description")
                .define("msgAdvancement", "%player% just gained the advancement **%name%**\\n_%desc%_");
        msgChatMessage = builder
                .comment("Chat message when webhook is disabled", "PLACEHOLDERS:", "%player% - The player\u00B4s name", "%msg% - The chat message")
                .define("msgChatMessage", "%player%: %msg%");
        description = builder
                .comment("Channel description while the server is online", "PLACEHOLDERS:", "%online% - Online player amount", "%max% - Maximum player count", "%tps% - Server TPS", "%motd% - The server MOTD (from server.properties!)", "%uptime% - The uptime of the server")
                .define("description", "%motd% (%online%/%max%) | %tps% TPS | Uptime: %uptime%");
        descriptionOffline = builder
                .comment("Channel description while the server is offline")
                .define("descriptionOffline", "Server is Offline!");
        descriptionStarting = builder
                .comment("Channel description while the server is starting")
                .define("descriptionStarting", "Starting...");
        msgPlayerTimeout = builder
                .comment("PLACEHOLDERS:", "%player% - The player\u00B4s name", "NOTE: This is currently not implemented because mixins are not working in 1.14!")
                .define("msgPlayerTimeout", "%player% timed out!");
        sayOutput = builder.comment("Should /say output be sent to discord?")
                .define("enableSayOutput", true);
        builder.pop();
        //#########################
        //#       COMMANDS        #
        //#########################
        builder.comment("Configuration for built-in discord commands").push("dc-commands");
        final String defaultCommandJson;
        //Default command json
        {
            final JsonObject a = new JsonObject();
            final JsonObject kick = new JsonObject();
            kick.addProperty("adminOnly", true);
            kick.addProperty("mcCommand", "kick");
            kick.addProperty("description", "Kicks a player from the server");
            kick.addProperty("useArgs", true);
            kick.addProperty("argText", "<player> [reason]");
            a.add("kick", kick);
            final JsonObject stop = new JsonObject();
            stop.addProperty("adminOnly", true);
            stop.addProperty("mcCommand", "stop");
            stop.addProperty("description", "Stops the server");
            final JsonArray stopAliases = new JsonArray();
            stopAliases.add("shutdown");
            stop.add("aliases", stopAliases);
            stop.addProperty("useArgs", false);
            a.add("stop", stop);
            final JsonObject kill = new JsonObject();
            kill.addProperty("adminOnly", true);
            kill.addProperty("mcCommand", "kill");
            kill.addProperty("description", "Kills a player");
            kill.addProperty("useArgs", true);
            kill.addProperty("argText", "<player>");
            a.add("kill", kill);
            final Gson gson = new GsonBuilder().create();
            defaultCommandJson = gson.toJson(a);
        }
        adminRoleId = builder
                .comment("The Role ID of your Admin Role")
                .define("adminRoleId", "0");
        prefix = builder
                .comment("The prefix of the commands like list")
                .define("prefix", "/");
        msgListEmpty = builder
                .comment("The message for 'list' when no player is online")
                .define("msgListEmpty", "There is no player online...");
        msgListOne = builder
                .comment("The message for 'list' when one player is online")
                .define("msgListOne", "There is 1 player online:");
        msgListHeader = builder
                .comment("The header for 'list'", "PLACEHOLDERS:", "%amount% - The amount of players online")
                .define("msgListHeader", "There are %amount% players online:");
        msgNoPermission = builder
                .comment("Message sent when user does not have permission to run a command")
                .define("msgNoPermission", "You don\u00B4t have permission to execute this command!");
        msgUnknownCommand = builder
                .comment("Message sent when an invalid command was typed", "PLACEHOLDERS:", "%prefix% - Command prefix")
                .define("msgUnknownCommand", "Unknown command, try `%prefix%help` for a list of commands");
        msgNotEnoughArgs = builder
                .comment("Message if a player provides less arguments than required")
                .define("msgNotEnoughArgs", "Not enough arguments");
        msgTooManyArgs = builder
                .comment("Message if a player provides too many arguments")
                .define("msgTooManyArgs", "Too many arguments");
        msgPlayerNotFound = builder
                .comment("Message if a player can not be found", "PLACEHOLDERS:", "%player% - The player\u00B4s name")
                .define("msgPlayerNotFound", "Can not find player \"%player%\"");
        jsonCommands = builder
                .comment("Add your Custom commands to this JSON",
                        "You can copy-paste it to https://jsoneditoronline.org  Make sure when pasting here, that the json is NOT mulitlined.",
                        "You can click on \"Compact JSON Data\" on the website",
                        "NOTE: You MUST op the uuid set at SENDER_UUID in the ops.txt !!!",
                        "",
                        "mcCommand   -   The command to execute on the server",
                        "adminOnly   -   True: Only allows users with the Admin role to use this command. False: @everyone can use the command",
                        "description -   Description shown in /help",
                        "aliases     -   Aliases for the command in a string array",
                        "useArgs     -   Shows argument text after the command",
                        "argText     -   Defines custom arg text. Default is <args>")
                .define("jsonCommands", defaultCommandJson);
        senderUUID = builder.comment("You MUST op this UUID in the ops.txt or many commands won´t work!!")
                .define("senderUUID", "8d8982a5-8cf9-4604-8feb-3dd5ee1f83a3");
        builder.pop();
        //#########################
        //#    INGAME-COMMAND     #
        //#########################
        builder.comment("Configurate the /discord command useable ingame").push("dc-commands");
        dcCmdEnabled = builder
                .comment("Enable the /discord command?")
                .define("dcCmdEnabled", true);
        dcCmdMsg = builder
                .comment("The message displayed when typing /discord in the server chat")
                .define("dcCmdMsg", "Join our discord! http://discord.gg/myserver");
        dcCmdMsgHover = builder
                .comment("The message shown when hovering the /discord command message")
                .define("dcCmdMsgHover", "Click to open the invite url");
        dcCmdURL = builder
                .comment("The url to open when clicking the /discord command text")
                .define("dcCmdURL", "http://discord.gg/myserver");
        builder.pop();
//		//#########################
//		//#     FTB-UTILITIES     #
//		//#########################
//		builder.comment("Theese config values will only be used when FTB Utilities is installed!").push("ftbutilities");
//		ftbuAfkMsgEnabled = builder
//				.comment("Print afk messages in discord?")
//				.define("ftbuAfkMsgEnabled", true);
//		ftbuAFKMsg = builder
//				.comment("Format of the AFK message", "PLACEHOLDERS:", "%player% - The player\u00B4s name")
//				.define("ftbuAFKMsg", "%player% is now AFK");
//		ftbuAFKMsgEnd = builder
//				.comment("Format of the no longer AFK message", "PLACEHOLDERS:", "%player% - The player\u00B4s name")
//				.define("ftbuAFKMsgEnd", "%player% is no longer AFK");
//		ftbuAvatar = builder
//				.comment("URL of the FTB Avatar icon")
//				.define("ftbuAvatar", "https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/master/images/ftb.png");
//		ftbuShutdownMsg = builder
//				.comment("Format of the shutdown message printed when the server will shutdown/restart in 30 and 10 seconds","PLACEHOLDERS:", "%seconds% - The seconds remaining till shutdown (30 or 10)")
//				.define("ftbuShutdownMsg", "Server stopping in %seconds%!");
//		builder.pop();
    }

    public static void setValueAndSave(final ModConfig cfg, final List<String> list, final Object newValue) {
        cfg.getConfigData().set(list, newValue);
        cfg.save();
    }
}
