package de.erdbeerbaerlp.dcintegration.common.util;

import net.dv8tion.jda.api.entities.Emote;
import net.kyori.adventure.text.Component;

import java.util.List;

public abstract class ServerInterface {
    public abstract int getMaxPlayers();

    public abstract int getOnlinePlayers();

    public abstract void sendMCMessage(Component msg);

    public abstract String formatEmoteMessage(List<Emote> emotes, String msg);
}
