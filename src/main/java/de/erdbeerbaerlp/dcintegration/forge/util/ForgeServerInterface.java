package de.erdbeerbaerlp.dcintegration.forge.util;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dcshadow.com.vdurmont.emoji.EmojiParser;
import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.TextReplacementConfig;
import dcshadow.net.kyori.adventure.text.event.ClickEvent;
import dcshadow.net.kyori.adventure.text.event.HoverEvent;
import dcshadow.net.kyori.adventure.text.format.Style;
import dcshadow.net.kyori.adventure.text.format.TextColor;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import dcshadow.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.ComponentUtils;
import de.erdbeerbaerlp.dcintegration.common.util.McServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import de.erdbeerbaerlp.dcintegration.forge.DiscordIntegrationMod;
import de.erdbeerbaerlp.dcintegration.forge.command.DCCommandSender;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ForgeServerInterface implements McServerInterface {

    @Override
    public int getMaxPlayers() {
        return ServerLifecycleHooks.getCurrentServer() == null ? -1 : ServerLifecycleHooks.getCurrentServer().getMaxPlayers();
    }

    @Override
    public int getOnlinePlayers() {
        return ServerLifecycleHooks.getCurrentServer() == null ? -1 : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerCount();
    }

    @Override
    public void sendIngameMessage(Component msg) {
        final List<ServerPlayer> l = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        try {
            for (final ServerPlayer p : l) {
                if (!playerHasPermissions(p.getUUID(), MinecraftPermission.READ_MESSAGES, MinecraftPermission.USER))
                    return;
                if (!DiscordIntegration.INSTANCE.ignoringPlayers.contains(p.getUUID()) && !(LinkManager.isPlayerLinked(p.getUUID()) && LinkManager.getLink(null, p.getUUID()).settings.ignoreDiscordChatIngame)) {
                    final Map.Entry<Boolean, Component> ping = ComponentUtils.parsePing(msg, p.getUUID(), p.getName().getString());
                    final String jsonComp = GsonComponentSerializer.gson().serialize(ping.getValue()).replace("\\\\n", "\n");
                    final net.minecraft.network.chat.Component comp = ComponentArgument.textComponent().parse(new StringReader(jsonComp));
                    p.sendSystemMessage(comp);
                    if (ping.getKey()) {
                        if (LinkManager.isPlayerLinked(p.getUUID()) && LinkManager.getLink(null, p.getUUID()).settings.pingSound) {
                            p.connection.send(new ClientboundSoundPacket(SoundEvents.NOTE_BLOCK_PLING, SoundSource.MASTER, p.position().x, p.position().y, p.position().z, 1, 1, 1));
                        }
                    }
                }
            }
            //Send to server console too
            final String jsonComp = GsonComponentSerializer.gson().serialize(msg).replace("\\\\n", "\n");
            final net.minecraft.network.chat.Component comp = ComponentArgument.textComponent().parse(new StringReader(jsonComp));
            ServerLifecycleHooks.getCurrentServer().sendSystemMessage(comp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendIngameReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, EmojiUnion reactionEmote) {
        final List<ServerPlayer> l = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        for (final ServerPlayer p : l) {
            if (p.getUUID().equals(targetUUID) && !DiscordIntegration.INSTANCE.ignoringPlayers.contains(p.getUUID()) && (LinkManager.isPlayerLinked(p.getUUID()) && !LinkManager.getLink(null, p.getUUID()).settings.ignoreDiscordChatIngame && !LinkManager.getLink(null, p.getUUID()).settings.ignoreReactions)) {
                final String emote = reactionEmote.getType() == Emoji.Type.UNICODE ? EmojiParser.parseToAliases(reactionEmote.getName()) : ":" + reactionEmote.getName() + ":";

                Style.Builder memberStyle = Style.style();
                if (Configuration.instance().messages.discordRoleColorIngame)
                    memberStyle = memberStyle.color(TextColor.color(member.getColorRaw()));

                final Component user = Component.text(member.getEffectiveName()).style(memberStyle
                        .clickEvent(ClickEvent.suggestCommand("<@" + member.getId() + ">"))
                        .hoverEvent(HoverEvent.showText(Component.text(Localization.instance().discordUserHover.replace("%user#tag%", member.getUser().getAsTag()).replace("%user%", member.getEffectiveName()).replace("%id%", member.getUser().getId())))));
                final TextReplacementConfig userReplacer = ComponentUtils.replaceLiteral("%user%", user);
                final TextReplacementConfig emoteReplacer = ComponentUtils.replaceLiteral("%emote%", emote);

                final Component out = LegacyComponentSerializer.legacySection().deserialize(Localization.instance().reactionMessage)
                        .replaceText(userReplacer).replaceText(emoteReplacer);

                if (Localization.instance().reactionMessage.contains("%msg%"))
                    retrieveMessage.submit().thenAccept((m) -> {
                        final String msg = ForgeMessageUtils.formatEmoteMessage(m.getMentions().getCustomEmojis(), m.getContentDisplay());
                        final TextReplacementConfig msgReplacer = ComponentUtils.replaceLiteral("%msg%", msg);
                        sendReactionMCMessage(p, out.replaceText(msgReplacer));
                    });
                else sendReactionMCMessage(p, out);
            }
        }
    }

    @Override
    public void runMcCommand(String cmd, final CompletableFuture<InteractionHook> cmdMsg, User user) {

        final DCCommandSender s = new DCCommandSender(cmdMsg, user);
        try {
            ServerLifecycleHooks.getCurrentServer().getCommands().getDispatcher().execute(cmd.trim(), s.createCommandSourceStack());
        } catch (CommandSyntaxException e) {
            s.sendSystemMessage(net.minecraft.network.chat.Component.literal(e.getMessage()));
        }
    }

    @Override
    public HashMap<UUID, String> getPlayers() {
        final HashMap<UUID, String> players = new HashMap<>();
        for (ServerPlayer p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            players.put(p.getUUID(), p.getDisplayName().getString().isEmpty() ? p.getName().getString() : p.getDisplayName().getString());
        }
        return players;
    }

    @Override
    public void sendIngameMessage(String msg, UUID player) {
        final ServerPlayer p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(player);
        if (p != null)
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
    }

    @Override
    public boolean isOnlineMode() {
        return Configuration.instance().bungee.isBehindBungee || ServerLifecycleHooks.getCurrentServer().usesAuthentication();
    }

    private void sendReactionMCMessage(ServerPlayer target, Component msgComp) {
        if (!playerHasPermissions(target.getUUID(), MinecraftPermission.READ_MESSAGES, MinecraftPermission.USER))
            return;
        final String jsonComp = GsonComponentSerializer.gson().serialize(msgComp).replace("\\\\n", "\n");
        try {
            final net.minecraft.network.chat.Component comp = ComponentArgument.textComponent().parse(new StringReader(jsonComp));
            target.sendSystemMessage(comp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNameFromUUID(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer().getSessionService().fillProfileProperties(new GameProfile(uuid, ""), false).getName();
    }

    @Override
    public String getLoaderName() {
        return "Forge";
    }

    @Override
    public boolean playerHasPermissions(UUID player, String... permissions) {
        final ServerPlayer serverPlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(player);
        for (String p : permissions) {
            if (serverPlayer != null) {
                if (PermissionAPI.getPermission(serverPlayer, DiscordIntegrationMod.nodes.get(p))) {
                    return true;
                }
            } else {
                if (PermissionAPI.getOfflinePermission(player, DiscordIntegrationMod.nodes.get(p))) {
                    return true;
                }
            }
        }
        return false;
    }
}
