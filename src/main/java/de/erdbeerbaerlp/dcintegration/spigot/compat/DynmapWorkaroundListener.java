package de.erdbeerbaerlp.dcintegration.spigot.compat;

import de.erdbeerbaerlp.dcintegration.spigot.DiscordIntegration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.dynmap.DynmapWebChatEvent;

public class DynmapWorkaroundListener implements Listener {

    /**
     * Required as workaround for an bug with the webChatEvent in DynmapCommonAPIListener
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWebChat(DynmapWebChatEvent ev){
        DiscordIntegration.INSTANCE.dynmapListener.sendMessage(ev.getName(),ev.getMessage());
    }
}
