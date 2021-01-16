package de.erdbeerbaerlp.dcintegration.spigot.util;

import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class SpigotMessageUtils extends MessageUtils {

    public static String formatPlayerName(Map.Entry<UUID, String> p) {
            return ChatColor.stripColor(p.getValue());
    }

    public static String formatPlayerName(Player player) {
        return formatPlayerName(new DefaultMapEntry<>(player.getUniqueId(), player.getName()));
    }

    public static BaseComponent[] adventureToSpigot(final Component comp){
        return ComponentSerializer.parse(GsonComponentSerializer.gson().serialize(comp));
    }

    public static Component spigotToAdventure(final BaseComponent[] comp){
        return GsonComponentSerializer.gson().deserialize(ComponentSerializer.toString(comp));
    }

}
