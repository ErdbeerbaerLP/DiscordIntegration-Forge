package de.erdbeerbaerlp.dcintegration.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.erdbeerbaerlp.dcintegration.Discord;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
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
    public final ConfigValue<String> botToken;
    public final ConfigValue<String> botPresenceName;
    public final ForgeConfigSpec.EnumValue<Discord.GameTypes> botPresenceType;
    public final ConfigValue<String> botChannel;
    public final ForgeConfigSpec.BooleanValue botModifyDescription;
    public final ForgeConfigSpec.BooleanValue whitelist;
    public final ForgeConfigSpec.BooleanValue allowLink;
    //#########################
    //#        WEBHOOK        #
    //#########################
    public final ForgeConfigSpec.BooleanValue enableWebhook;
    public final ConfigValue<String> serverAvatar;
    public final ConfigValue<String> serverName;
    //#########################
    //#       MESSAGES        #
    //#########################
    public final ForgeConfigSpec.BooleanValue discordColorCodes;
    public final ForgeConfigSpec.BooleanValue preventMcColorCodes;
    public final ConfigValue<String> msgServerStarted;
    public final ConfigValue<String> msgServerStarting;
    public final ConfigValue<String> msgServerStopped;
    public final ConfigValue<String> msgPlayerJoin;
    public final ConfigValue<String> msgPlayerLeave;
    public final ConfigValue<String> msgPlayerDeath;
    public final ConfigValue<String> msgServerCrash;
    public final ConfigValue<String> ingameDiscordMsg;
    public final ConfigValue<String> msgAdvancement;
    public final ConfigValue<String> msgChatMessage;
    public final ConfigValue<String> description;
    public final ConfigValue<String> descriptionOffline;
    public final ConfigValue<String> descriptionStarting;
    public final ConfigValue<String> msgPlayerTimeout;
    public final ForgeConfigSpec.BooleanValue sayOutput;
    public final ForgeConfigSpec.BooleanValue meOutput;
    public final ConfigValue<String> imcModIdBlacklist;
    public final ForgeConfigSpec.BooleanValue tamedDeathEnabled;
    public final ConfigValue<String> msgIgnoreUnignore;
    public final ConfigValue<String> msgIgnoreIgnore;
    public final ConfigValue<String> uptimeFormat;

    //#########################
    //#       COMMANDS        #
    //#########################
    public final ConfigValue<String> adminRoleId;
    public final ConfigValue<String> prefix;
    public final ConfigValue<String> msgListEmpty;
    public final ConfigValue<String> msgListOne;
    public final ConfigValue<String> msgListHeader;
    public final ConfigValue<String> msgNoPermission;
    public final ConfigValue<String> msgUnknownCommand;
    public final ConfigValue<String> msgNotEnoughArgs;
    public final ConfigValue<String> msgTooManyArgs;
    public final ConfigValue<String> msgPlayerNotFound;
    public final ConfigValue<String> jsonCommands;
    public final ConfigValue<String> senderUUID;
    public final ForgeConfigSpec.BooleanValue cmdHelpEnabled;
    public final ForgeConfigSpec.BooleanValue cmdListEnabled;
    public final ForgeConfigSpec.BooleanValue cmdUptimeEnabled;
    public final ForgeConfigSpec.BooleanValue enableUnknownCommandEverywhere;
    public final ForgeConfigSpec.BooleanValue enableUnknownCommandMsg;
    public final ConfigValue<String> helpCmdChannelID;
    public final ConfigValue<String> listCmdChannelID;
    public final ConfigValue<String> uptimeCmdChannelID;
    public final ConfigValue<String> helpHeader;

    //#########################
    //#    INGAME-COMMAND     #
    //#########################
    public final ForgeConfigSpec.BooleanValue dcCmdEnabled;
    public final ConfigValue<String> dcCmdMsg;
    public final ConfigValue<String> dcCmdMsgHover;
    public final ConfigValue<String> dcCmdURL;

//	//#########################
//	//#     FTB-UTILITIES     # XXX Not implemented, since FTB Utilities is still on 1.12
//	//#########################
//	public final ForgeConfigSpec.BooleanValue ftbuAfkMsgEnabled;
//	public final ConfigValue<String> ftbuAFKMsg;
//	public final ConfigValue<String> ftbuAFKMsgEnd;
//	public final ConfigValue<String> ftbuAvatar;
//	public final ConfigValue<String> ftbuShutdownMsg;


    //#########################
    //#        ADVANCED       #
    //#########################
    public final ConfigValue<String> channelDescriptionID;
    public final ConfigValue<String> serverChannelID;
    public final ConfigValue<String> deathChannelID;
    public final ConfigValue<String> chatOutputChannel;
    public final ConfigValue<String> chatInputChannel;
    public final ConfigValue<String> msgNotWhitelisted;

    Configuration(final ForgeConfigSpec.Builder builder) {
        //#########################
        //#        GENERAL        #
        //#########################
        builder.comment("General bot Configuration").push("generalSettings");
        botToken = builder.comment("Insert your Bot Token here!", "DO NOT SHARE IT WITH ANYONE!").define("botToken", "INSERT BOT TOKEN HERE");
        botPresenceName = builder.comment("The Name of the Game", "", "PLACEHOLDERS:", "%online% - Online Players", "%max% - Maximum Player Amount").define("botPresenceName", "Minecraft with %online% players");
        botPresenceType = builder.defineEnum("botPresenceType", Discord.GameTypes.PLAYING);
        botChannel = builder.comment("The channel ID where the bot will be working in").define("botChannel", "000000000");
        botModifyDescription = builder.comment("Wether or not the Bot should modify the channel description").define("botModifyDescription", true);
        allowLink = builder.comment("Should discord linking be enabled?", "If whitelist is on, this can not be disabled").define("allow-linking", true);
        whitelist = builder.comment("Enable discord based whitelist?", "This will override the link config!", "To whitelist use !whitelist <uuid> in the bot DMs").define("whitelist", false);
        builder.pop();
        //#########################
        //#        WEBHOOK        #
        //#########################
        builder.comment("Webhook configuration").push("webhook");
        enableWebhook = builder.comment("Wether or not the bot should use a webhook (it will create one)").define("enableWebhook", false);
        serverAvatar = builder.comment("The avatar to be used for server messages").define("serverAvatar", "https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/master/images/srv.png");
        serverName = builder.comment("Wether or not the bot should use a webhook (it will create one)").define("serverName", "Server");
        builder.pop();
        //#########################
        //#       MESSAGES        #
        //#########################
        builder.comment("Customize messages of this mod").push("messages");
        discordColorCodes = builder.comment("Disable removal of color codes from chat to discord?").define("discordColorCodes", false);
        preventMcColorCodes = builder.comment("Enable removal of color codes from discord to chat?").define("preventMcColorCodes", false);
        msgServerStarted = builder.comment("This message will edited in / sent when the server finished starting").define("msgServerStarted", "Server Started!");
        msgServerStarting = builder.comment("Message to show while the server is starting", "This will be edited to SERVER_STARTED_MSG when webhook is false").define("msgServerStarting", "Server Starting...");
        msgServerStopped = builder.comment("This message will be sent when the server was stopped").define("msgServerStopped", "Server Stopped!");
        msgPlayerJoin = builder.comment("PLACEHOLDERS:", "%player% - The player\u00B4s name").define("msgPlayerJoin", "%player% joined");
        msgPlayerLeave = builder.comment("PLACEHOLDERS:", "%player% - The player\u00B4s name").define("msgPlayerLeave", "%player% left");
        msgPlayerDeath = builder.comment("PLACEHOLDERS:", "%player% - The player\u00B4s name", "%msg% - The death message").define("msgPlayerDeath", "%player% %msg%");
        msgServerCrash = builder.comment("The message to print to discord when it was possible to detect a server crash", "Will also be used in the channel description").define("msgServerCrash", "Server Crash Detected :thinking:");
        ingameDiscordMsg = builder.comment("This is what will be displayed ingame when someone types into the bot\u00B4s channel", "PLACEHOLDERS:", "%user% - The username", "%id% - The user ID", "%msg% - The Message").define(
                "ingameDiscordMsg", "\u00A76[\u00A75DISCORD\u00A76]\u00A7r <%user%> %msg%");
        msgAdvancement = builder.comment("Supports MulitLined messages using \\n", "PLACEHOLDERS:", "%player% - The player\u00B4s name", "%name% - The advancement name", "%desc% - The advancement description").define("msgAdvancement",
                "%player% just gained the advancement **%name%**\\n_%desc%_");
        msgChatMessage = builder.comment("Chat message", "PLACEHOLDERS:", "%player% - The player\u00B4s name", "%msg% - The chat message").define("msgChatMessage", "%player%: %msg%");
        description = builder.comment("Channel description while the server is online", "PLACEHOLDERS:", "%online% - Online player amount", "%max% - Maximum player count", "%tps% - Server TPS",
                "%motd% - The server MOTD (from server.properties!)", "%uptime% - The uptime of the server").define("description", "%motd% (%online%/%max%) | %tps% TPS | Uptime: %uptime%");
        descriptionOffline = builder.comment("Channel description while the server is offline").define("descriptionOffline", "Server is Offline!");
        descriptionStarting = builder.comment("Channel description while the server is starting").define("descriptionStarting", "Starting...");
        msgPlayerTimeout = builder.comment("PLACEHOLDERS:", "%player% - The player\u00B4s name", "NOTE: This is currently not implemented because mixins are not working in 1.15!").define("msgPlayerTimeout", "%player% timed out!");
        sayOutput = builder.comment("Should /say output be sent to discord?").define("enableSayOutput", true);
        meOutput = builder.comment("Should /me output be sent to discord?").define("enableMeOutput", true);
        imcModIdBlacklist = builder.comment("A list of blacklisted modids", "Adding one will prevent the mod to send messages to discord using forges IMC system").define("imcModIdBlacklist", parseArray(new String[]{"examplemodid"}));
        tamedDeathEnabled = builder.comment("Should tamed entity death be visible in discord?").define("tamedDeathEnabled", false);
        msgIgnoreIgnore = builder.comment("Message sent when ignoring Discord messages").define("msgIgnoreIgnore", "You are now ignoring Discord messages!");
        msgIgnoreUnignore = builder.comment("Message sent when unignoring Discord messages").define("msgIgnoreUnignore", "You are no longer ignoring Discord messages!");
        uptimeFormat = builder.comment("The format of the uptime command and %uptime% placeholder", "For more help with the formatting visit https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/time/DurationFormatUtils.html").define("uptimeFormat", "dd 'days' HH 'hours' mm 'minutes' ss 'seconds'");
        msgNotWhitelisted = builder.comment("Message shown to players who are not whitelisted using discord", "No effect if discord whitelist is off").define("msgWhitelist", TextFormatting.RED + "You are not whitelisted.\nJoin the discord server for more information\nhttps://discord.gg/someserver");
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
            final JsonObject tps = new JsonObject();
            tps.addProperty("adminOnly", false);
            tps.addProperty("mcCommand", "forge tps");
            tps.addProperty("description", "Displays TPS");
            tps.addProperty("useArgs", false);
            a.add("tps", tps);
            final Gson gson = new GsonBuilder().create();
            defaultCommandJson = gson.toJson(a);
        }
        adminRoleId = builder.comment("The Role ID of your Admin Role").define("adminRoleId", "0");
        prefix = builder.comment("The prefix of the commands like list").define("prefix", "/");
        msgListEmpty = builder.comment("The message for 'list' when no player is online").define("msgListEmpty", "There is no player online...");
        msgListOne = builder.comment("The header for 'list' when one player is online").define("msgListOne", "There is 1 player online:");
        msgListHeader = builder.comment("The header for 'list'", "PLACEHOLDERS:", "%amount% - The amount of players online").define("msgListHeader", "There are %amount% players online:");
        msgNoPermission = builder.comment("Message sent when user does not have permission to run a command").define("msgNoPermission", "You don\u00B4t have permission to execute this command!");
        msgUnknownCommand = builder.comment("Message sent when an invalid command was typed", "PLACEHOLDERS:", "%prefix% - Command prefix").define("msgUnknownCommand", "Unknown command, try `%prefix%help` for a list of commands");
        msgNotEnoughArgs = builder.comment("Message if a player provides less arguments than required").define("msgNotEnoughArgs", "Not enough arguments");
        msgTooManyArgs = builder.comment("Message if a player provides too many arguments").define("msgTooManyArgs", "Too many arguments");
        msgPlayerNotFound = builder.comment("Message if a player can not be found", "PLACEHOLDERS:", "%player% - The player\u00B4s name").define("msgPlayerNotFound", "Can not find player \"%player%\"");
        cmdHelpEnabled = builder.comment("Enable help command?", "Disabling also removes response when you entered an invalid command", "Requires server restart").define("enableHelpCommand", true);
        cmdListEnabled = builder.comment("Enable the list command in discord", "Requires server restart").define("enableListCommand", true);
        cmdUptimeEnabled = builder.comment("Enable the iptime command in discord", "Requires server restart").define("enableUptimeCommand", true);
        jsonCommands = builder.comment("Add your Custom commands to this JSON", "You can copy-paste it to https://jsoneditoronline.org  Make sure when pasting here, that the json is NOT mulitlined.",
                "You can click on \"Compact JSON Data\" on the website", "NOTE: The JSON string must be escaped. You can use this website to escape or unescape: https://www.freeformatter.com/java-dotnet-escape.html",
                "NOTE 2: You MUST op the uuid set at SENDER_UUID in the " + "ops.txt !!!", "", "mcCommand   -   The command to" + " " + "execute on the " + "server",
                "adminOnly   -   True: Only allows users with the Admin role to use this command. False: @everyone can use the command", "description -   Description shown in /help",
                "aliases     -   Aliases for the command in a string array", "useArgs     -   Shows argument text after the command", "argText     -   Defines custom arg text. Default is <args>",
                "channelIDs  -   Allows you to set specific text channels outside" + " of the server channel to use this command (make it an string array), Set to [\"00\"] to allow from all channels").define(
                "jsonCommands", defaultCommandJson);
        senderUUID = builder.comment("You MUST op this UUID in the ops.txt or many commands won't work!!").define("senderUUID", "8d8982a5-8cf9-4604-8feb-3dd5ee1f83a3");
        enableUnknownCommandEverywhere = builder.comment("Set to true to enable the \"Unknown Command\" message in all channels").define("enableUnknownCommandEverywhere", false);
        enableUnknownCommandMsg = builder.comment("Set to false to completely disable the \"Unknown Command\" message").define("enableUnknownCommandMsg", true);
        helpHeader = builder.comment("Header of the help command").define("helpHeader", "Your available commands in this channel:");
        helpCmdChannelID = builder.comment("Custom Channel ID list for the help command. Set to 00 to allow usage from everywhere and to 0 to allow usage from the bots default channel").define("helpCmdChannel", "0");
        uptimeCmdChannelID = builder.comment("Custom Channel ID list for the uptime command. Set to 00 to allow usage from everywhere and to 0 to allow usage from the bots default channel").define("helpCmdChannel", "0");
        listCmdChannelID = builder.comment("Custom Channel ID list for the list command. Set to 00 to allow usage from everywhere and to 0 to allow usage from the bots default channel").define("helpCmdChannel", "0");

        builder.pop();
        //#########################
        //#    INGAME-COMMAND     #
        //#########################
        builder.comment("Configurate the /discord command useable ingame").push("mc-command");
        dcCmdEnabled = builder.comment("Enable the /discord command?").define("dcCmdEnabled", true);
        dcCmdMsg = builder.comment("The message displayed when typing /discord in the server chat").define("dcCmdMsg", "Join our discord! http://discord.gg/myserver");
        dcCmdMsgHover = builder.comment("The message shown when hovering the /discord command message").define("dcCmdMsgHover", "Click to open the invite url");
        dcCmdURL = builder.comment("The url to open when clicking the /discord command text").define("dcCmdURL", "http://discord.gg/myserver");
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
        //#########################
        //#        ADVANCED       #
        //#########################
        builder.comment("Configure Advanced features like moving specific message types to different channels").push("advanced");
        channelDescriptionID = builder.comment("Custom channel for description", "Leave empty to use default channel").define("channelDescriptionID", "");
        serverChannelID = builder.comment("Custom channel ID for server specific messages (like Join/leave)", "Leave empty to use default channel").define("serverChannelID", "");
        deathChannelID = builder.comment("Custom channel ID for death messages", "Leave empty to use default channel").define("deathChannelID", "");
        chatOutputChannel = builder.comment("Custom channel for for ingame messages", "Leave empty to use default channel").define("chatOutputID", "");
        chatInputChannel = builder.comment("Custom channel where messages get sent to minecraft", "Leave empty to use default channel").define("chatInputID", "");
        builder.pop();
    }

    public static String parseArray(String[] array) {
        String out = "[";
        for (int i = 0; i < array.length; i++) {
            out += "\"" + array[i] + "\"";
            if (i != array.length - 1) out += ", ";
        }
        out += "]";
        return out;
    }

    public static String[] getArray(String s) {
        final ArrayList<String> tmpList = new ArrayList<>();
        if (!s.startsWith("[") && !s.endsWith("]")) return new String[0];
        s = s.replace("[", "").replace("]", "");
        for (final String a : s.split(", ")) {
            tmpList.add(a.replace("\"", ""));
        }
        return tmpList.toArray(new String[0]);
    }

    public static void setValueAndSave(final ModConfig cfg, final List<String> list, final Object newValue) {
        cfg.getConfigData().set(list, newValue);
        cfg.save();
    }
}
