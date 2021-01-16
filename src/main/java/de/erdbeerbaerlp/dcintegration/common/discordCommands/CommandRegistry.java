package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat.*;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs.*;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandRegistry {
    /**
     * Registered commands for bot DMs
     */
    private static final List<DMCommand> dmCommands = new ArrayList<>();
    /**
     * Registered commands
     */
    private static List<DiscordCommand> commands = new ArrayList<>();

    /**
     * Registers all default commands and custom commands from config
     */
    public static void registerDefaultCommandsFromConfig() {
        if (Configuration.instance().commands.helpCmdEnabled)
            registerCommand(new CommandHelp());
        if (Configuration.instance().commands.listCmdEnabled)
            registerCommand(new CommandList());
        if (Configuration.instance().commands.uptimeCmdEnabled)
            registerCommand(new CommandUptime());

        if (Configuration.instance().linking.enableLinking) {
            registerCommand(new DMHelpCommand());
            registerCommand(new SettingsCommand());
            if (Configuration.instance().linking.whitelistMode)
                registerCommand(new WhitelistCommand());
            else
                registerCommand(new LinkCommand());
        }
        registerConfigCommands();
    }

    /**
     * Registers all custom commands from config
     */
    public static void registerConfigCommands() {
        final JsonObject commandJson = new JsonParser().parse(Configuration.instance().commands.customCommandJSON).getAsJsonObject();
        System.out.println("Detected to load " + commandJson.size() + " commands to load from config");
        for (Map.Entry<String, JsonElement> cmd : commandJson.entrySet()) {
            final JsonObject cmdVal = cmd.getValue().getAsJsonObject();
            if (!cmdVal.has("mcCommand")) {
                System.err.println("Skipping command " + cmd.getKey() + " because it is invalid! Check your config!");
                continue;
            }
            final String mcCommand = cmdVal.get("mcCommand").getAsString();
            final String desc = cmdVal.has("description") ? cmdVal.get("description").getAsString() : "No Description";
            final boolean admin = !cmdVal.has("adminOnly") || cmdVal.get("adminOnly").getAsBoolean();
            final boolean useArgs = !cmdVal.has("useArgs") || cmdVal.get("useArgs").getAsBoolean();
            String argText = "<args>";
            if (cmdVal.has("argText")) argText = cmdVal.get("argText").getAsString();
            String[] aliases = new String[0];
            if (cmdVal.has("aliases") && cmdVal.get("aliases").isJsonArray()) {
                aliases = new String[cmdVal.getAsJsonArray("aliases").size()];
                for (int i = 0; i < aliases.length; i++)
                    aliases[i] = cmdVal.getAsJsonArray("aliases").get(i).getAsString();
            }
            String[] channelID = (cmdVal.has("channelID") && cmdVal.get("channelID") instanceof JsonArray) ? MessageUtils.makeStringArray(cmdVal.get("channelID").getAsJsonArray()) : new String[]{"0"};
            final DiscordCommand regCmd = new CommandFromCFG(cmd.getKey(), desc, mcCommand, admin, aliases, useArgs, argText, channelID);
            if (!registerCommand(regCmd))
                System.err.println("Failed Registering command \"" + cmd.getKey() + "\" because it would override an existing command!");
        }
        System.out.println("Finished registering! Registered " + commands.size() + " commands");
    }

    /**
     * Registers an {@link DiscordCommand} or {@link DMCommand}
     *
     * @param cmd command
     * @return if the registration was successful
     */
    public static boolean registerCommand(@Nonnull DiscordCommand cmd) {
        if (cmd instanceof DMCommand) {
            final ArrayList<DMCommand> toRemove = new ArrayList<>();
            for (DMCommand c : dmCommands) {
                if (!cmd.isConfigCommand() && cmd.equals(c)) return false;
                else if (cmd.isConfigCommand() && cmd.equals(c)) toRemove.add(c);
            }
            for (DMCommand cm : toRemove)
                dmCommands.remove(cm);
            return dmCommands.add((DMCommand) cmd);
        } else {
            final ArrayList<DiscordCommand> toRemove = new ArrayList<>();
            for (DiscordCommand c : commands) {
                if (!cmd.isConfigCommand() && cmd.equals(c)) return false;
                else if (cmd.isConfigCommand() && cmd.equals(c)) toRemove.add(c);
            }
            for (DiscordCommand cm : toRemove)
                commands.remove(cm);
            return commands.add(cmd);
        }
    }

    /**
     * Attempts to reload all commands
     */
    public static void reRegisterAllCommands() {
        final List<DiscordCommand> cmds = commands;
        System.out.println("Reloading " + cmds.size() + " commands");
        commands = new ArrayList<>();

        for (DiscordCommand cmd : cmds) {
            if (cmd.isConfigCommand()) continue;
            commands.add(cmd);
        }

        System.out.println("Registered " + commands.size() + " commands");

        final List<DMCommand> dmcmds = dmCommands;
        System.out.println("Reloading " + dmcmds.size() + " DM commands");
        commands = new ArrayList<>();
        for (DMCommand cmd : dmcmds) {
            if (cmd.isConfigCommand()) continue;
            commands.add(cmd);
        }
        System.out.println("Registered " + dmCommands.size() + " commands");
    }

    /**
     * @return A list of all registered commands
     */
    @Nonnull
    public static List<DiscordCommand> getCommandList() {
        return commands;
    }

    /**
     * @return List of registered DM commands
     */
    @Nonnull
    public static List<DMCommand> getDMCommandList() {
        return dmCommands;
    }
}
