package de.erdbeerbaerlp.dcintegration.forge.util;

import com.mojang.brigadier.StringReader;
import com.vdurmont.emoji.EmojiParser;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.ServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.entities.Emote;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.command.arguments.ComponentArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.List;

public class ForgeServerInterface extends ServerInterface {

    @Override
    public int getMaxPlayers() {
        return ServerLifecycleHooks.getCurrentServer() == null ? -1 : ServerLifecycleHooks.getCurrentServer().getMaxPlayers();
    }

    @Override
    public int getOnlinePlayers() {
        return ServerLifecycleHooks.getCurrentServer() == null ? -1 : ServerLifecycleHooks.getCurrentServer().getOnlinePlayerNames().length;
    }

    @Override
    public void sendMCMessage(Component msg) {
        final List<ServerPlayerEntity> l = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        final String jsonComp = GsonComponentSerializer.gson().serialize(msg).replace("\\\\n", "\n");
        try {
            final ITextComponent comp = ComponentArgument.component().parse(new StringReader(jsonComp));
            for (final ServerPlayerEntity p : l) {
                if (!Variables.discord_instance.ignoringPlayers.contains(p.getUniqueID()) && !(PlayerLinkController.isPlayerLinked(p.getUniqueID()) && PlayerLinkController.getSettings(null, p.getUniqueID()).ignoreDiscordChatIngame)) {
                    p.sendMessage(comp, Util.DUMMY_UUID);
                }
            }
            //Send to server console too
            ServerLifecycleHooks.getCurrentServer().sendMessage(comp, Util.DUMMY_UUID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String formatEmoteMessage(List<Emote> emotes, String msg) {
        msg = EmojiParser.parseToAliases(msg);
        for (final Emote e : emotes) {
            msg = msg.replace(e.toString(), ":" + e.getName() + ":");
        }
        return msg;
    }
}
