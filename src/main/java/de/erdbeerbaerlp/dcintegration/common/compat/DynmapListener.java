package de.erdbeerbaerlp.dcintegration.common.compat;

import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;

public class DynmapListener extends DynmapCommonAPIListener {

    private final boolean workaroundEnabled;
    private final DynmapSender sender = new DynmapSender();
    private DynmapCommonAPI api;

    public DynmapListener() {
        this.workaroundEnabled = false;
    }

    public DynmapListener(boolean workaroundEnabled) {

        this.workaroundEnabled = workaroundEnabled;
    }

    @Override
    public void apiEnabled(DynmapCommonAPI api) {
        this.api = api;
        if (Variables.discord_instance != null)
            Variables.discord_instance.registerEventHandler(sender);
        System.out.println("Dynmap listener registered");
    }

    @Override
    public void apiDisabled(DynmapCommonAPI api) {
        if (Variables.discord_instance != null)
            Variables.discord_instance.unregisterEventHandler(sender);
    }


    @Override
    public boolean webChatEvent(String source, String name, String message) {
        if(!this.workaroundEnabled)
            sendMessage(name, message);
        return super.webChatEvent(source, name, message);
    }

    public void sendMessage(String name, String message){
        Variables.discord_instance.sendMessage(Variables.discord_instance.getChannel(Configuration.instance().dynmap.dynmapChannelID), Configuration.instance().dynmap.dcMessage.replace("%msg%", message).replace("%sender%", name.isEmpty() ? Configuration.instance().dynmap.unnamed : name), Configuration.instance().dynmap.avatarURL, Configuration.instance().dynmap.name);
    }

    public void register() {
        DynmapCommonAPIListener.register(this);
    }

    public class DynmapSender extends DiscordEventHandler {
        @Override
        public void onDiscordMessagePost(MessageReceivedEvent event) {
            api.sendBroadcastToWeb(Configuration.instance().dynmap.webName.replace("%name#tag%", event.getAuthor().getAsTag()).replace("%name%", event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName()), event.getMessage().getContentDisplay());
        }
    }

}
