package de.erdbeerbaerlp.dcintegration.integrations;
/*  :( No votifier in 1.15 yet
import com.github.upcraftlp.votifier.api.VoteReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


public class VotifierEventHandler
{
    @SubscribeEvent
    public void voteEvent(VoteReceivedEvent ev) {
        if (DiscordIntegration.discord_instance != null && Configuration.VOTIFIER.ENABLED)
            DiscordIntegration.discord_instance.sendMessage(Configuration.ADVANCED.VOTIFIER_CHANNEL_ID.isEmpty() ? DiscordIntegration.discord_instance.getChannel() : DiscordIntegration.discord_instance.getChannel(Configuration.ADVANCED.VOTIFIER_CHANNEL_ID), Configuration.VOTIFIER.MESSAGE.replace("%player%", DiscordIntegration
                    .formatPlayerName(ev.getEntityPlayer(), false)).replace("%addr%", ev.getRemoteAddress()).replace("%site%", ev.getServiceDescriptor()), Configuration.VOTIFIER.AVATAR_URL, Configuration.VOTIFIER.NAME);
    }
}
*/