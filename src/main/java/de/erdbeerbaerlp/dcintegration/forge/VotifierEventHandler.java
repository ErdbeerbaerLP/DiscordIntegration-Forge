package de.erdbeerbaerlp.dcintegration.forge;

import com.github.upcraftlp.votifier.api.VoteReceivedEvent;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.forge.util.ForgeMessageUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.AbstractMap;


public class VotifierEventHandler
{
    @SubscribeEvent
    public void voteEvent(VoteReceivedEvent ev) {
        if (Variables.discord_instance != null && Configuration.instance().votifier.enabled)
            Variables.discord_instance.sendMessage(Configuration.instance().votifier.votifierChannelID.isEmpty() ? Variables.discord_instance.getChannel() : Variables.discord_instance.getChannel(Configuration.instance().votifier.votifierChannelID), Configuration.instance().votifier.message.replace("%player%", ForgeMessageUtils
                    .formatPlayerName(new AbstractMap.SimpleEntry<>(ev.getEntityPlayer().getUniqueID(),ev.getEntityPlayer().getName()), false)).replace("%addr%", ev.getRemoteAddress()).replace("%site%", ev.getServiceDescriptor()), Configuration.instance().votifier.avatarURL, Configuration.instance().votifier.name);
    }
}