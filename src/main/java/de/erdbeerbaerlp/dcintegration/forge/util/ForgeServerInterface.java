package de.erdbeerbaerlp.dcintegration.forge.util;

import com.mojang.authlib.GameProfile;
import dcshadow.dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import de.erdbeerbaerlp.dcintegration.common.DiscordEventListener;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.ComponentUtils;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.ServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.forge.command.DCCommandSender;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

public class ForgeServerInterface extends ServerInterface {

    @Override
    public int getMaxPlayers() {
        return FMLCommonHandler.instance().getMinecraftServerInstance() == null ? -1 : FMLCommonHandler.instance().getMinecraftServerInstance().getMaxPlayers();
    }

    @Override
    public int getOnlinePlayers() {
        return FMLCommonHandler.instance().getMinecraftServerInstance() == null ? -1 : FMLCommonHandler.instance().getMinecraftServerInstance().getOnlinePlayerNames().length;
    }

    @Override
    public void sendMCMessage(Component msg) {
        final List<EntityPlayerMP> l = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
        try {
            for (final EntityPlayerMP p : l) {
                if (!Variables.discord_instance.ignoringPlayers.contains(p.getUniqueID()) && !(PlayerLinkController.isPlayerLinked(p.getUniqueID()) && PlayerLinkController.getSettings(null, p.getUniqueID()).ignoreDiscordChatIngame)) {
                    final Map.Entry<Boolean, Component> ping = ComponentUtils.parsePing(msg, p.getUniqueID(), p.getName());
                    final String jsonComp = GsonComponentSerializer.gson().serialize(ping.getValue()).replace("\\\\n", "\n");
                    final ITextComponent comp =  ITextComponent.Serializer.jsonToComponent(jsonComp);
                    p.sendMessage(comp);
                    if (ping.getKey()) {
                        if (PlayerLinkController.getSettings(null, p.getUniqueID()).pingSound) {
                            p.connection.sendPacket(new SPacketSoundEffect(SoundEvents.BLOCK_NOTE_PLING, SoundCategory.MASTER, p.posX, p.posY, p.posZ, 1, 1));
                        }
                    }
                }
            }
            //Send to server console too
            final String jsonComp = GsonComponentSerializer.gson().serialize(msg).replace("\\\\n", "\n");
            final ITextComponent comp =  ITextComponent.Serializer.jsonToComponent(jsonComp);
            FMLCommonHandler.instance().getMinecraftServerInstance().sendMessage(comp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMCReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, MessageReaction.ReactionEmote reactionEmote) {
        final List<EntityPlayerMP> l = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
        for (final EntityPlayerMP p : l) {
            if (p.getUniqueID().equals(targetUUID) && !Variables.discord_instance.ignoringPlayers.contains(p.getUniqueID()) && !PlayerLinkController.getSettings(null, p.getUniqueID()).ignoreDiscordChatIngame && !PlayerLinkController.getSettings(null, p.getUniqueID()).ignoreReactions) {

                final String emote = reactionEmote.isEmote() ? ":" + reactionEmote.getEmote().getName() + ":" : MessageUtils.formatEmoteMessage(new ArrayList<>(), reactionEmote.getEmoji());
                String outMsg = Configuration.instance().localization.reactionMessage.replace("%name%", member.getEffectiveName()).replace("%name2%", member.getUser().getAsTag()).replace("%emote%", emote);
                if (Configuration.instance().localization.reactionMessage.contains("%msg%"))
                    retrieveMessage.submit().thenAccept((m) -> {
                        String outMsg2 = outMsg.replace("%msg%", m.getContentDisplay());
                        sendReactionMCMessage(p, ForgeMessageUtils.formatEmoteMessage(m.getEmotes(), outMsg2));
                    });
                else sendReactionMCMessage(p, outMsg);
            }
        }
    }

    @Override
    public void runMcCommand(String cmd, MessageReceivedEvent cmdMsg) {
        final DCCommandSender s = new DCCommandSender(cmdMsg.getAuthor(), cmdMsg.getTextChannel().getId());
        if (s.canUseCommand(4, "")) {
            FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager().executeCommand(s, cmd.trim());

        } else
            discord_instance.sendMessage("Sorry, but the bot has no permissions...\nAdd this into the servers ops.json:\n```json\n {\n   \"uuid\": \"" + Configuration.instance().commands.senderUUID + "\",\n   \"name\": \"DiscordFakeUser\",\n   \"level\": 4,\n   \"bypassesPlayerLimit\": false\n }\n```", cmdMsg.getTextChannel());

    }

    @Override
    public HashMap<UUID, String> getPlayers() {
        final HashMap<UUID, String> players = new HashMap<>();
        for (EntityPlayerMP p : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
            players.put(p.getUniqueID(), p.getDisplayName().getUnformattedComponentText().isEmpty() ? p.getName() : p.getDisplayName().getUnformattedComponentText());
        }
        return players;
    }

    @Override
    public void sendMCMessage(String msg, UUID player) {
        FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(player).sendMessage(new TextComponentString(msg));

    }

    @Override
    public boolean isOnlineMode() {
        return Configuration.instance().bungee.isBehindBungee ||  FMLCommonHandler.instance().getMinecraftServerInstance().isServerInOnlineMode();
    }

    private void sendReactionMCMessage(EntityPlayerMP target, String msg) {
        final Component msgComp = MinecraftSerializer.INSTANCE.serialize(msg.replace("\n", "\\n"), DiscordEventListener.mcSerializerOptions);
        final String jsonComp = GsonComponentSerializer.gson().serialize(msgComp).replace("\\\\n", "\n");
        try {
            final ITextComponent comp =  ITextComponent.Serializer.jsonToComponent(jsonComp);
            target.sendMessage(comp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNameFromUUID(UUID uuid) {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getMinecraftSessionService().fillProfileProperties(new GameProfile(uuid, ""), false).getName();
    }
}
