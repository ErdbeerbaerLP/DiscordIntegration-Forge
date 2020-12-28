package de.erdbeerbaerlp.dcintegration.spigot.util;

import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class SpigotMessageUtils extends MessageUtils {

    public static String formatPlayerName(Map.Entry<UUID, String> p) {
        return formatPlayerName(p, true);
    }

    public static String formatPlayerName(Map.Entry<UUID, String> p, boolean chatFormat) {
        final String discordName = getDiscordName(p.getKey());
        if (discordName != null)
            return discordName;
        else
            return ChatColor.stripColor(p.getValue());
    }

    public static String formatPlayerName(Player player) {
        return formatPlayerName(new DefaultMapEntry<>(player.getUniqueId(), player.getDisplayName().isEmpty() ? player.getName() : player.getDisplayName()));
    }

}
