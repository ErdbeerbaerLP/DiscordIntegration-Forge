package de.erdbeerbaerlp.dcintegration.common.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlComment;
import com.moandjiezana.toml.TomlIgnore;
import com.moandjiezana.toml.TomlWriter;
import de.erdbeerbaerlp.dcintegration.common.util.GameType;
import de.erdbeerbaerlp.dcintegration.common.util.UpdateChecker;

import java.io.IOException;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.configFile;

public class Configuration {

    @TomlIgnore
    private static final String defaultCommandJson;
    @TomlIgnore
    private static Configuration INSTANCE;

    static {
        //Default command json
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
        /*final JsonObject tps = new JsonObject();
        tps.addProperty("adminOnly", false);
        tps.addProperty("mcCommand", "forge tps");
        tps.addProperty("description", "Displays TPS");
        tps.addProperty("useArgs", false);
        a.add("tps", tps);*/
        final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        defaultCommandJson = gson.toJson(a);


        //First instance of the Config
        INSTANCE = new Configuration();
        INSTANCE.loadConfig();
    }

    @TomlComment("General options for the bot")
    public General general = new General();

    @TomlComment("Configuration options for commands")
    public Commands commands = new Commands();

    @TomlComment("Toggle some message related features")
    public Messages messages = new Messages();

    @TomlComment("Advanced options")
    public Advanced advanced = new Advanced();

    @TomlComment("Config options which only have an effect when using forge")
    public ForgeSpecific forgeSpecific = new ForgeSpecific();

    @TomlComment("Configuration for linking")
    public Configuration.Linking linking = new Configuration.Linking();

    @TomlComment("Webhook configuration")
    public Configuration.Webhook webhook = new Configuration.Webhook();

    @TomlComment("Allows you to modify and translate most of the messages this bot will send")
    public Configuration.Localization localization = new Configuration.Localization();

    @TomlComment("Configuration for the in-game command '/discord'")
    public Configuration.IngameCommand ingameCommand = new Configuration.IngameCommand();

    @TomlComment("The command log channel is an channel where every command execution gets logged")
    public CommandLog commandLog = new CommandLog();

    @TomlComment({"Configure votifier integration here", "(Spigot only)"})
    public Votifier votifier = new Votifier();

    @TomlComment("Configure Dynmap integration here")
    public Dynmap dynmap = new Dynmap();

    public static Configuration instance() {
        return INSTANCE;
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            INSTANCE = new Configuration();
            INSTANCE.saveConfig();
            return;
        }
        INSTANCE = new Toml().read(configFile).to(Configuration.class);
        INSTANCE.saveConfig(); //Re-write the config so new values get added after updates
    }

    public void saveConfig() {
        try {
            if (!configFile.exists()) {
                if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }
            final TomlWriter w = new TomlWriter.Builder()
                    .indentValuesBy(2)
                    .indentTablesBy(4)
                    .padArrayDelimitersBy(2)
                    .build();
            w.write(this, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class General {
        @TomlComment({"Insert your Bot Token here!", "DO NOT SHARE IT WITH ANYONE!"})
        public String botToken = "INSERT BOT TOKEN HERE";

        @TomlComment("The channel ID where the bot will be working in")
        public String botChannel = "000000000";

        @TomlComment({"The bot's status message", "", "PLACEHOLDERS:", "%online% - Online Players", "%max% - Maximum Player Amount"})
        public String botStatusName = "%online% players Online";

        @TomlComment({"Type of the bot's status", "Allowed Values: DISABLED,PLAYING,WATCHING,LISTENING"})
        public GameType botStatusType = GameType.PLAYING;

        @TomlComment({"Enable checking for updates?", "Notification will be shown after every server start in log when update is available"})
        public boolean enableUpdateChecker = true;

        @TomlComment({"The minimum release type for the update checker to notify", "Allowed values: release, beta, alpha"})
        public UpdateChecker.ReleaseType updateCheckerMinimumReleaseType = UpdateChecker.ReleaseType.beta;
    }

    public static class Messages {
        @TomlComment("Enable formatting conversion (Markdown <==> Minecraft)")
        public boolean convertCodes = true;

        @TomlComment({"Send formatting codes from mc chat to discord", "Has no effect when markdown <==> Minecraft is enabled"})
        public boolean formattingCodesToDiscord = false;

        @TomlComment("Should /say output be sent to discord?")
        public boolean sendOnSayCommand = true;

        @TomlComment("Should /me output be sent to discord?")
        public boolean sendOnMeCommand = true;

        @TomlComment("When an /say command's message starts with this prefix, it will not be sent to discord")
        public String sayCommandIgnoredPrefix = "\u00a74\u00a76\u00a7k\u00a7r";

        @TomlComment("Should tamed entity death be visible in discord?")
        public boolean sendDeathMessagesForTamedAnimals = false;


    }

    public static class Commands {
        @TomlComment({"The Role IDs of your Admin Roles", "Now supports multiple roles which can access admin commands"})
        public String[] adminRoleIDs = new String[0];

        @TomlComment("The prefix of the commands usable in discord")
        public String prefix = "/";

        @TomlComment("Set to true to require an space after the prefix (e.g. 'mc help')")
        public boolean spaceAfterPrefix = false;

        @TomlComment({"Add your Custom commands to this JSON",
                "You can copy-paste it to https://jsoneditoronline.org  Make sure when pasting here, that the json is NOT mulitlined.",
                "You can click on \"Compact JSON Data\" on the website",
                "NOTE: The JSON string must be escaped. You can use this website to escape or unescape: https://www.freeformatter.com/java-dotnet-escape.html",
                "",
                "mcCommand   -   The command to execute on the server. use %args% to place the arguments inside of the command",
                "adminOnly   -   True: Only allows users with the Admin role to use this command. False: @everyone can use the command",
                "description -   Description shown in /help",
                "aliases     -   Aliases for the command in a string array",
                "useArgs     -   Shows argument text after the command in the help command",
                "argText     -   Defines custom arg text. Default is <args>",
                "channelIDs  -   Allows you to set specific text channels outside of the server channel to use this command (make it an string array), Set to [\"00\"] to allow from all channels"})
        public String customCommandJSON = defaultCommandJson;

        @TomlComment("You must op this UUID in the ops.txt or some custom commands won't work!")
        public String senderUUID = "8d8982a5-8cf9-4604-8feb-3dd5ee1f83a3";

        @TomlComment({"Enable help command?", "Disabling also removes response when you entered an invalid command", "Requires server restart"})
        public boolean helpCmdEnabled = true;

        @TomlComment({"Enable the list command in discord", "Requires server restart"})
        public boolean listCmdEnabled = true;

        @TomlComment({"Enable the uptime command in discord", "Requires server restart"})
        public boolean uptimeCmdEnabled = true;

        @TomlComment("Set to false to completely disable the \"Unknown Command\" message")
        public boolean showUnknownCommandMessage = true;

        @TomlComment("Set to true to enable the \"Unknown Command\" message in all channels")
        public boolean showUnknownCommandEverywhere = false;

    }

    public static class Advanced {
        @TomlComment({"Custom channel ID for server specific messages (like Join/leave)", "Leave 'default' to use default channel"})
        public String serverChannelID = "default";

        @TomlComment({"Custom channel ID for death messages", "Leave 'default' to use default channel"})
        public String deathsChannelID = "default";

        @TomlComment({"Custom channel for for ingame messages", "Leave 'default' to use default channel"})
        public String chatOutputChannelID = "default";

        @TomlComment({"Custom channel where messages get sent to minecraft", "Leave 'default' to use default channel"})
        public String chatInputChannelID = "default";

        @TomlComment("Custom Channel ID list for the help command. Set to 00 to allow usage from everywhere and to 0 to allow usage from the bots default channel")
        public String[] helpCmdChannelIDs = new String[]{"0"};

        @TomlComment("Custom Channel ID list for the list command. Set to 00 to allow usage from everywhere and to 0 to allow usage from the bots default channel")
        public String[] listCmdChannelIDs = new String[]{"0"};

        @TomlComment("Custom Channel ID list for the uptime command. Set to 00 to allow usage from everywhere and to 0 to allow usage from the bots default channel")
        public String[] uptimeCmdChannelIDs = new String[]{"0"};
    }

    public static class ForgeSpecific {
        @TomlComment({"A list of blacklisted modids", "Adding one will prevent the mod to send messages to discord using forges IMC system"})
        public String[] IMC_modIdBlacklist = new String[]{"examplemod"};

        @TomlComment("Show item information, which is visible on hover ingame, as embed in discord?")
        public boolean sendItemInfo = true;
    }

    public static class IngameCommand {
        @TomlComment("Enable the /discord command to show an custom message with invite URL?")
        public boolean enabled = true;

        @TomlComment("The message displayed when typing /discord in the server chat")
        public String message = "Join our discord! http://discord.gg/myserver";

        @TomlComment("The message shown when hovering the /discord command message")
        public String hoverMessage = "Click to open the invite url";

        @TomlComment("The url to open when clicking the /discord command text")
        public String inviteURL = "http://discord.gg/myserver";
    }

    public static class Localization {
        @TomlComment({"This is what will be displayed ingame when someone types into the bot's channel", "PLACEHOLDERS:", "%user% - The username", "%id% - The user ID", "%msg% - The Message"})
        public String ingame_discordMessage = "\u00a76[\u00a75DISCORD\u00a76]\u00a7r <%user%> %msg%";

        @TomlComment("This message will edited in / sent when the server finished starting")
        public String serverStarted = "Server Started!";

        @TomlComment({"Message to show while the server is starting", "This will be edited to SERVER_STARTED_MSG when webhook is false"})
        public String serverStarting = "Server Starting...";

        @TomlComment("This message will be sent when the server was stopped")
        public String serverStopped = "Server Stopped!";

        @TomlComment("The message to print to discord when it was possible to detect a server crash")
        public String serverCrash = "Server Crash Detected :thinking:";

        @TomlComment({"Gets sent when an player joins", "", "PLACEHOLDERS:", "%player% - The player's name"})
        public String playerJoin = "%player% joined";

        @TomlComment({"Gets sent when an player leaves", "", "PLACEHOLDERS:", "%player% - The player's name"})
        public String playerLeave = "%player% left";

        @TomlComment({"Gets sent when an player dies", "", "PLACEHOLDERS:", "%player% - The player's name", "%msg% - The death message"})
        public String playerDeath = "%player% %msg%";

        @TomlComment({"Message sent instead of playerLeave, when the player times out", "", "PLACEHOLDERS:", "%player% - The player's name"})
        public String playerTimeout = "%player% timed out!";

        @TomlComment({"Gets sent when an player finishes an advancement", "Supports MulitLined messages using \\n", "", "PLACEHOLDERS:", "%player% - The player's name", "%name% - The advancement name", "%desc% - The advancement description"})
        public String advancementMessage = "%player% just made the advancement **%name%**\\n_%desc%_";

        @TomlComment({"The chat message in discord, sent from an player in-game", "", "PLACEHOLDERS:", "%player% - The player's name", "%msg% - The chat message"})
        public String discordChatMessage = "%player%: %msg%";


        @TomlComment({"Sent to a player when someone reacts to his messages", "PLACEHOLDERS:", "%name% - (Nick-)Name of the user who reacted (format: 'SomeNickName')", "%name2% - Name of the user who reacted with discord discriminator (format: 'SomeName#0123')", "%msg% - Content of the message which got the reaction", "%emote% - The reacted emote"})
        public String reactionMessage = "\u00a76[\u00a75DISCORD\u00a76]\u00a7r\u00a77 %name% reacted to your message \"\u00a79%msg%\u00a77\" with '%emote%'";


        @TomlComment("Strings about the discord commands")
        public Commands commands = new Commands();

        @TomlComment("Strings about the account linking feature")
        public Linking linking = new Linking();

        @TomlComment("Strings about the personal settings feature")
        public PersonalSettings personalSettings = new PersonalSettings();

        public static class Linking {

            @TomlComment({"Sent to the user when he linked his discord successfully", "PLACEHOLDERS:", "%player% - The in-game player name", "%prefix% - Command prefix"})
            public String linkSuccessful = "Your account is now linked with %player%.\nUse %prefix%settings here to view and set some user-specific settings";

            @TomlComment({"Sent to the user when linking fails"})
            public String linkFailed = "Account link failed";

            @TomlComment({"Sent when an already linked user attempts to link an account", "PLACEHOLDERS:", "%player% - The in-game player name"})
            public String alreadyLinked = "Your account is already linked with %player%";

            @TomlComment({"Sent when attempting to use personal commands while not linked", "PLACEHOLDERS:", "%method% - The currently enabled method for linking"})
            public String notLinked = "Your account is not linked! Link it first using %method%";

            @TomlComment({"Message of the link method in whitelist mode", "Used by %method% placeholder"})
            public String linkMethodWhitelist = "`%prefix%whitelist <uuid>` here";

            @TomlComment({"Message of the link method in normal mode", "Used by %method% placeholder"})
            public String linkMethodIngame = "`/discord link` ingame";

            @TomlComment({"Sent when attempting to whitelist-link with an non uuid string", "PLACEHOLDERS:", "%arg% - The provided argument", "%prefix% - Command prefix"})
            public String link_argumentNotUUID = "Argument \"%arg%\" is not an valid UUID. Use `%prefix%whitelist <uuid>`";

            @TomlComment("Sent when attempting to link with an unknown number")
            public String invalidLinkNumber = "Invalid link number! Use `/discord link` ingame to get your link number";

            @TomlComment("Sent when attempting to link with an invalid number")
            public String linkNumberNAN = "This is not a number. Use `/discord link` ingame to get your link number";

            @TomlComment({"Message shown to players who are not whitelisted using discord", "No effect if discord whitelist is off"})
            public String notWhitelisted = "\u00a7cYou are not whitelisted.\nJoin the discord server for more information\nhttps://discord.gg/someserver";

            @TomlComment("Sent when trying to link without an required role")
            public String link_requiredRole = "You need to have an role to use this";

            @TomlComment("Sent when trying to link as an non-member")
            public String link_notMember = "You are not member of the Discord-Server this bot is operating in!";
            @TomlComment({"Sent to the user when he linked his discord successfully", "PLACEHOLDERS:", "%name% - The linked discord name", "%name#tag% - The linked discord name with tag"})
            public String linkSuccessfulIngame = "Your account is now linked with discord-user %name#tag%";
            @TomlComment({"Message shown to players who want to link their discord account ingame", "", "PLACEHOLDERS:", "%num% - The link number", "%prefix% - Command prefix"})
            public String linkMsgIngame = "Send this command as a direct message to the bot to link your account: %prefix%link %num%\nThis number will expire after 10 minutes";

            @TomlComment("Shown when hovering over the link message")
            public String hoverMsg_copyClipboard = "Click to copy command to clipboard";
        }

        public static class Commands {

            public String ingameOnly = "This command can only be executed ingame";

            @TomlComment("Shown when successfully reloading the config file")
            public String configReloaded = "Config reloaded!";

            @TomlComment("Shown when an subcommand is disabled")
            public String subcommandDisabled = "This subcommand is disabled!";

            @TomlComment("Message sent when user does not have permission to run a command")
            public String noPermission = "You don't have permission to execute this command!";

            @TomlComment({"Message sent when an invalid command was typed", "", "PLACEHOLDERS:", "%prefix% - Command prefix"})
            public String unknownCommand = "Unknown command, try `%prefix%help` for a list of commands";

            @TomlComment("Message if a player provides less arguments than required")
            public String notEnoughArguments = "Not enough arguments";

            @TomlComment("Message if a player provides too many arguments")
            public String tooManyArguments = "Too many arguments";

            @TomlComment({"Message if a player can not be found", "", "PLACEHOLDERS:", "%player% - The player's name"})
            public String playerNotFound = "Can not find player \"%player%\"";

            @TomlComment("The message for 'list' when no player is online")
            public String cmdList_empty = "There is no player online...";

            @TomlComment("The header for 'list' when one player is online")
            public String cmdList_one = "There is 1 player online:";

            @TomlComment({"The header for 'list'", "PLACEHOLDERS:", "%amount% - The amount of players online"})
            public String cmdList_header = "There are %amount% players online:";

            @TomlComment("Header of the help command")
            public String cmdHelp_header = "Your available commands in this channel:";

            @TomlComment("Message sent when ignoring Discord messages")
            public String commandIgnore_ignore = "You are now ignoring Discord messages!";

            @TomlComment("Message sent when unignoring Discord messages")
            public String commandIgnore_unignore = "You are no longer ignoring Discord messages!";

            @TomlComment({"Message sent when using the uptime command", "", "PLACEHOLDERS:", "%uptime% - Uptime in uptime format, see uptimeFormat"})
            public String cmdUptime_message = "The server is running for %uptime%";

            @TomlComment({"The format of the uptime command", "For more help with the formatting visit https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/time/DurationFormatUtils.html"})
            public String uptimeFormat = "dd 'days' HH 'hours' mm 'minutes'";
            @TomlComment("Command descriptions")
            public Descriptions descriptions = new Descriptions();

            public static class Descriptions {
                public String settings = "Allows you to edit your personal settings";
                public String uptime = "Displays the server uptime";
                public String help = "Displays a list of all commands";
                public String list = "Lists all players currently online";
                public String link = "Links your Discord account with your Minecraft account";
                public String whitelist = "Whitelists you on the server by linking with Discord";
            }
        }

        public static class PersonalSettings {

            @TomlComment("Message for getting an setting's value")
            public String personalSettingGet = "This settings value is `%bool%`";

            @TomlComment("Sent when user sucessfully updates an prersonal setting")
            public String settingUpdateSuccessful = "Successfully updated setting!";

            @TomlComment("Header of the personal settings list")
            public String personalSettingsHeader = "Personal Settings list:";

            @TomlComment("Error message when providing an invalid personal setting name")
            public String invalidPersonalSettingKey = "`%key%` is not an valid setting!";

            @TomlComment({"PLACEHOLDERS:", "%prefix% - Returns the current command prefix"})
            public String settingsCommandUsage = "Usages:\n\n%prefix%settings - lists all available keys\n%prefix%settings get <key> - Gets the current settings value\n%prefix%settings set <key> <value> - Sets an Settings value";

            @TomlComment("Sent when setting an personal setting fails")
            public String settingUpdateFailed = "Failed to set value :/";

            @TomlComment("Descriptions of the settings")
            public Descriptions descriptons = new Descriptions();

            public static class Descriptions {
                public String ignoreDiscordChatIngame = "Configure if you want to ignore discord chat ingame";
                public String useDiscordNameInChannel = "Should the bot send messages using your discord name and avatar instead of your in-game name and skin?";
                public String ignoreReactions = "Configure if you want to ignore discord reactions ingame";
                public String pingSound = "Toggle the ingame ping sound";
            }
        }
    }


    public static class Webhook {
        @TomlComment("Whether or not the bot should use a webhook (it will create one)")
        public boolean enable = false;

        @TomlComment("The avatar to be used for server messages")
        public String serverAvatarURL = "https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/master/images/srv.png";

        @TomlComment("The name to be used for server messages")
        public String serverName = "Minecraft Server";

        @TomlComment({"The URL where the player avatar gets fetched from", "", "PLACEHOLDERS:", "%uuid% - Returns the player's UUID with dashes", "%uuid_dashless% - Returns the player's UUID without dashes", "%name% - Returns the player's name", "%randomUUID% - Returns an random UUID which can be used to prevent discord cache"})
        public String playerAvatarURL = "https://minotar.net/avatar/%uuid%?randomuuid=%randomUUID%";
    }

    public static class Linking {
        @TomlComment({"Should discord linking be enabled?", "If whitelist is on, this can NOT be disabled", "DOES NOT WORK IN OFFLINE MODE!"})
        public boolean enableLinking = true;

        @TomlComment({"Role ID of an role an player should get when he links his discord account", "Leave as 0 to disable"})
        public String linkedRoleID = "0";

        @TomlComment({"Enable discord based whitelist?", "This will override the link config!", "To whitelist use !whitelist <uuid> in the bot DMs"})
        public boolean whitelistMode = false;

        @TomlComment("Adding Role IDs here will require the players to have at least ONE of these roles to link account")
        public String[] requiredRoles = new String[0];

        @TomlComment("Allows you to configure the default values of some personal settings")
        PersonalSettingsDefaults personalSettingsDefaults = new PersonalSettingsDefaults();

        public static class PersonalSettingsDefaults {
            public boolean default_useDiscordNameInChannel = true;
            public boolean default_ignoreReactions = false;
            public boolean default_pingSound = true;
        }
    }

    public static class CommandLog {
        @TomlComment({"Channel ID for the command log channel", "Leave 0 to disable"})
        public String channelID = "0";

        @TomlComment({"The format of the log messages", "", "PLACEHOLDERS:", "%sender% - The name of the Command Source", "%cmd% - executed command (e.g. \"/say Hello World\"", "%cmd-no-args% - Command without arguments (e.g. \"/say\""})
        public String message = "%sender% executed command `%cmd%`";
    }

    public static class Votifier {
        @TomlComment("Should votifier messages be sent to discord?")
        public boolean enabled = true;

        @TomlComment({"Custom channel ID for Votifier messages", "Leave 'default' to use default channel"})
        public String votifierChannelID = "default";

        @TomlComment({"The message format of the votifier message", "", "PLACEHOLDERS:", "%player% - The player\u00B4s name", "%site% - The name of the vote site", "%addr% - (IP) Address of the site"})
        public String message = ":ballot_box: %player% just voted on %site%";

        @TomlComment("Name of the webhook title")
        public String name = "Votifier";

        @TomlComment("URL of the webhook avatar image")
        public String avatarURL = "https://www.cubecraft.net/attachments/bkjvmqn-png.126824/";
    }

    public static class Dynmap {
        @TomlComment({"The message format of the message forwarded to discord", "", "PLACEHOLDERS:", "%sender% - The sender\u00B4s name", "%msg% - The Message"})
        public String dcMessage = "<%sender%> %msg%";

        @TomlComment({"Custom channel ID for dynmap chat", "Leave 'default' to use default channel"})
        public String dynmapChannelID = "default";

        @TomlComment("Name of the webhook title")
        public String name = "Dynmap Web-Chat";

        @TomlComment("URL of the webhook avatar image")
        public String avatarURL = "https://styles.redditmedia.com/t5_2kl3ct/styles/communityIcon_am5zopqnjhs41.png";

        @TomlComment({"The name format of the message forwarded to the dynmap webchat", "", "PLACEHOLDERS:", "%name% - The discord name of the sender (including nickname)", "%name#tag% - The discord name with tag of the sender (without nickname)"})
        public String webName = "%name% (discord)";
        @TomlComment("Name shown in discord when no name was specified on the website")
        public String unnamed = "Unnamed";
    }
}
