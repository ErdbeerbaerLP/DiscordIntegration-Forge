package de.erdbeerbaerlp.dcintegration.forge.util;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dcshadow.dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import de.erdbeerbaerlp.dcintegration.common.DiscordEventListener;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
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
import net.minecraft.command.arguments.ComponentArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlaySoundPacket;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ForgeServerInterface implements ServerInterface {

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
        try {
            for (final ServerPlayerEntity p : l) {
                if (!Variables.discord_instance.ignoringPlayers.contains(p.getUniqueID()) && !(PlayerLinkController.isPlayerLinked(p.getUniqueID()) && PlayerLinkController.getSettings(null, p.getUniqueID()).ignoreDiscordChatIngame)) {
                    final Map.Entry<Boolean, Component> ping = ComponentUtils.parsePing(msg, p.getUniqueID(), p.getName().getUnformattedComponentText());
                    final String jsonComp = GsonComponentSerializer.gson().serialize(ping.getValue()).replace("\\\\n", "\n");
                    final ITextComponent comp = ComponentArgument.component().parse(new StringReader(jsonComp));
                    p.sendMessage(comp, Util.DUMMY_UUID);
                    if (ping.getKey()) {
                        if (PlayerLinkController.getSettings(null, p.getUniqueID()).pingSound) {
                            p.connection.sendPacket(new SPlaySoundPacket(SoundEvents.BLOCK_NOTE_BLOCK_PLING.getRegistryName(), SoundCategory.MASTER, new Vector3d(p.getPosX(), p.getPosY(), p.getPosZ()), 1, 1));
                        }
                    }
                }
            }
            //Send to server console too
            final String jsonComp = GsonComponentSerializer.gson().serialize(msg).replace("\\\\n", "\n");
            final ITextComponent comp = ComponentArgument.component().parse(new StringReader(jsonComp));
            ServerLifecycleHooks.getCurrentServer().sendMessage(comp, Util.DUMMY_UUID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMCReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, MessageReaction.ReactionEmote reactionEmote) {
        final List<ServerPlayerEntity> l = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        for (final ServerPlayerEntity p : l) {
            if (p.getUniqueID().equals(targetUUID) && !Variables.discord_instance.ignoringPlayers.contains(p.getUniqueID()) && !PlayerLinkController.getSettings(null, p.getUniqueID()).ignoreDiscordChatIngame && !PlayerLinkController.getSettings(null, p.getUniqueID()).ignoreReactions) {

                final String emote = reactionEmote.isEmote() ? ":" + reactionEmote.getEmote().getName() + ":" : MessageUtils.formatEmoteMessage(new ArrayList<>(), reactionEmote.getEmoji());
                String outMsg = Localization.instance().reactionMessage.replace("%name%", member.getEffectiveName()).replace("%name2%", member.getUser().getAsTag()).replace("%emote%", emote);
                if (Localization.instance().reactionMessage.contains("%msg%"))
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
        if (s.hasPermissionLevel(4)) {
            try {
                ServerLifecycleHooks.getCurrentServer().getCommandManager().getDispatcher().execute(cmd.trim(), s.getCommandSource());
            } catch (CommandSyntaxException e) {
                s.sendMessage(new StringTextComponent(e.getMessage()), Util.DUMMY_UUID);
            }
        } else
            s.sendMessage(new StringTextComponent("Sorry, but the bot has no permissions...\nAdd this into the servers ops.json:\n```json\n {\n   \"uuid\": \"" + Configuration.instance().commands.senderUUID + "\",\n   \"name\": \"DiscordFakeUser\",\n   \"level\": 4,\n   \"bypassesPlayerLimit\": false\n }\n```"), Util.DUMMY_UUID);

    }

    @Override
    public HashMap<UUID, String> getPlayers() {
        final HashMap<UUID, String> players = new HashMap<>();
        for (ServerPlayerEntity p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            players.put(p.getUniqueID(), p.getDisplayName().getUnformattedComponentText().isEmpty() ? p.getName().getUnformattedComponentText() : p.getDisplayName().getUnformattedComponentText());
        }
        return players;
    }

    @Override
    public void sendMCMessage(String msg, UUID player) {
        final ServerPlayerEntity p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(player);
        if (p != null)
            p.sendMessage(new StringTextComponent(msg), Util.DUMMY_UUID);

    }

    @Override
    public boolean isOnlineMode() {
        return Configuration.instance().bungee.isBehindBungee || ServerLifecycleHooks.getCurrentServer().isServerInOnlineMode();
    }

    private void sendReactionMCMessage(ServerPlayerEntity target, String msg) {
        final Component msgComp = MinecraftSerializer.INSTANCE.serialize(msg.replace("\n", "\\n"), DiscordEventListener.mcSerializerOptions);
        final String jsonComp = GsonComponentSerializer.gson().serialize(msgComp).replace("\\\\n", "\n");
        try {
            final ITextComponent comp = ComponentArgument.component().parse(new StringReader(jsonComp));
            target.sendMessage(comp, Util.DUMMY_UUID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNameFromUUID(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer().getMinecraftSessionService().fillProfileProperties(new GameProfile(uuid, ""), false).getName();
    }
}
