package de.erdbeerbaerlp.dcintegration.common.util;

import de.erdbeerbaerlp.dcintegration.common.Discord;
import net.dv8tion.jda.api.entities.Message;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class Variables {

    /**
     * Mod/Plugin version
     */
    public static final String VERSION = "2.1.1";
    /**
     * Discord Integration data directory
     */
    public static final File discordDataDir = new File("./DiscordIntegration-Data/");
    /**
     * Time in milliseconds when the server started
     */
    public static long started;
    /**
     * Path to the config file
     */
    public static File configFile = new File("./config/Discord-Integration.toml");
    /**
     * Message sent when the server is starting (in non-webhook mode!), stored for editing
     */
    public static CompletableFuture<Message> startingMsg;
    /**
     * The currently active {@link Discord} instance
     */
    public static Discord discord_instance;
}
