package de.erdbeerbaerlp.dcintegration.spigot.api;

import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public abstract class SpigotDiscordEventHandler extends DiscordEventHandler {
    /**
     * Gets called before an minecraft message gets sent to discord
     *
     * @return true to cancel default code execution
     */
    public abstract boolean onMcChatMessage(AsyncPlayerChatEvent event);
}
