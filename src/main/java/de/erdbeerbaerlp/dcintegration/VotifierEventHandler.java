package de.erdbeerbaerlp.dcintegration;
/*  :( No votifier in 1.14 yet
import com.github.upcraftlp.votifier.api.VoteReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


public class VotifierEventHandler
{
    @SubscribeEvent
    public void voteEvent(VoteReceivedEvent ev) {
        if (DiscordIntegration.discord_instance != null && Configuration.VOTIFIER.ENABLED) DiscordIntegration.discord_instance.sendMessage(Configuration.VOTIFIER.MESSAGE.replace("%player%", DiscordIntegration
                .formatPlayerName(ev.getEntityPlayer(), false)).replace("%addr%", ev.getRemoteAddress()).replace("%site%", ev.getServiceDescriptor()), Configuration.VOTIFIER.AVATAR_URL, Configuration.VOTIFIER.NAME, null);
    }
}
*/