package de.erdbeerbaerlp.dcintegration.forge.util;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.minecraft.Util;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ForgeServerInterface extends ServerInterface {

    @Override
    public int getMaxPlayers() {
        return ServerLifecycleHooks.getCurrentServer() == null ? -1 : ServerLifecycleHooks.getCurrentServer().getMaxPlayers();
    }

    @Override
    public int getOnlinePlayers() {
        return ServerLifecycleHooks.getCurrentServer() == null ? -1 : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerCount();
    }

    @Override
    public void sendMCMessage(Component msg) {
        final List<ServerPlayer> l = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        try {
            for (final ServerPlayer p : l) {
                if (!Variables.discord_instance.ignoringPlayers.contains(p.getUUID()) && !(PlayerLinkController.isPlayerLinked(p.getUUID()) && PlayerLinkController.getSettings(null, p.getUUID()).ignoreDiscordChatIngame)) {
                    final Map.Entry<Boolean, Component> ping = ComponentUtils.parsePing(msg, p.getUUID(), p.getName().getContents());
                    final String jsonComp = GsonComponentSerializer.gson().serialize(ping.getValue()).replace("\\\\n", "\n");
                    final net.minecraft.network.chat.Component comp = ComponentArgument.textComponent().parse(new StringReader(jsonComp));
                    p.sendMessage(comp, Util.NIL_UUID);
                    if (ping.getKey()) {
                        if (PlayerLinkController.getSettings(null, p.getUUID()).pingSound) {
                            p.connection.send(new ClientboundSoundPacket(SoundEvents.NOTE_BLOCK_PLING, SoundSource.MASTER, p.position().x,p.position().y,p.position().z, 1, 1));
                        }
                    }
                }
            }
            //Send to server console too
            final String jsonComp = GsonComponentSerializer.gson().serialize(msg).replace("\\\\n", "\n");
            final net.minecraft.network.chat.Component comp = ComponentArgument.textComponent().parse(new StringReader(jsonComp));
            ServerLifecycleHooks.getCurrentServer().sendMessage(comp, Util.NIL_UUID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMCReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, MessageReaction.ReactionEmote reactionEmote) {
        final List<ServerPlayer> l = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        for (final ServerPlayer p : l) {
            if (p.getUUID().equals(targetUUID) && !Variables.discord_instance.ignoringPlayers.contains(p.getUUID()) && !PlayerLinkController.getSettings(null, p.getUUID()).ignoreDiscordChatIngame && !PlayerLinkController.getSettings(null, p.getUUID()).ignoreReactions) {

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
    public void runMcCommand(String cmd, final CompletableFuture<InteractionHook> cmdMsg, User user) {
        final DCCommandSender s = new DCCommandSender(cmdMsg, user);
        if (s.hasPermissions(4)) {
            try {
                ServerLifecycleHooks.getCurrentServer().getCommands().getDispatcher().execute(cmd.trim(), s.createCommandSourceStack());
            } catch (CommandSyntaxException e) {
                s.sendMessage(new TextComponent(e.getMessage()), Util.NIL_UUID);
            }
        } else
            s.sendMessage(new TextComponent("Sorry, but the bot has no permissions...\nAdd this into the servers ops.json:\n```json\n {\n   \"uuid\": \"" + Configuration.instance().commands.senderUUID + "\",\n   \"name\": \"DiscordFakeUser\",\n   \"level\": 4,\n   \"bypassesPlayerLimit\": false\n }\n```"), Util.NIL_UUID);

    }

    @Override
    public HashMap<UUID, String> getPlayers() {
        final HashMap<UUID, String> players = new HashMap<>();
        for (ServerPlayer p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            players.put(p.getUUID(), p.getDisplayName().getContents().isEmpty() ? p.getName().getContents() : p.getDisplayName().getContents());
        }
        return players;
    }

    @Override
    public void sendMCMessage(String msg, UUID player) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(player).sendMessage(new TextComponent(msg), Util.NIL_UUID);

    }

    @Override
    public boolean isOnlineMode() {
        return Configuration.instance().bungee.isBehindBungee || ServerLifecycleHooks.getCurrentServer().usesAuthentication();
    }

    private void sendReactionMCMessage(ServerPlayer target, String msg) {
        final Component msgComp = MinecraftSerializer.INSTANCE.serialize(msg.replace("\n", "\\n"), DiscordEventListener.mcSerializerOptions);
        final String jsonComp = GsonComponentSerializer.gson().serialize(msgComp).replace("\\\\n", "\n");
        try {
            final net.minecraft.network.chat.Component comp = ComponentArgument.textComponent().parse(new StringReader(jsonComp));
            target.sendMessage(comp, Util.NIL_UUID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public String getNameFromUUID(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer().getSessionService().fillProfileProperties(new GameProfile(uuid, ""), false).getName();
    }
}
