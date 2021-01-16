package de.erdbeerbaerlp.dcintegration.spigot.compat;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.spigot.util.SpigotMessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.FloodgateAPI;

import java.awt.*;


public class FloodgateLinkCommand {

    public static boolean link(Player p) {
        if (FloodgateAPI.isBedrockPlayer(p)) {
            if (Configuration.instance().linking.enableLinking && Variables.discord_instance.srv.isOnlineMode() && !Configuration.instance().linking.whitelistMode) {
                if (PlayerLinkController.isBedrockPlayerLinked(p.getUniqueId())) {
                    p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Configuration.instance().localization.linking.alreadyLinked.replace("%player%", Variables.discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromBedrockPlayer(p.getUniqueId())).getAsTag())).style(Style.style(TextColors.of(Color.RED)))));
                    return true;
                }
                final int r = Variables.discord_instance.genBedrockLinkNumber(p.getUniqueId());
                p.spigot().sendMessage(SpigotMessageUtils.adventureToSpigot(Component.text(Configuration.instance().localization.linking.linkMsgIngame.replace("%num%", r + "").replace("%prefix%", Configuration.instance().commands.prefix)).style(Style.style(TextColors.of(Color.ORANGE)))));
                return true;
            }
        }
        return false;
    }
}
