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
import de.erdbeerbaerlp.dcintegration.forge.command.DCCommandSender;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.minecraft.command.arguments.ComponentArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlaySoundPacket;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

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
        final List<ServerPlayerEntity> l = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        try {
            for (final ServerPlayerEntity p : l) {
                if (!DiscordIntegration.INSTANCE.ignoringPlayers.contains(p.getUUID()) && !(LinkManager.isPlayerLinked(p.getUUID()) && LinkManager.getLink(null, p.getUUID()).settings.ignoreDiscordChatIngame)) {
                    final Map.Entry<Boolean, Component> ping = ComponentUtils.parsePing(msg, p.getUUID(), p.getName().getString());
                    final String jsonComp = GsonComponentSerializer.gson().serialize(ping.getValue()).replace("\\\\n", "\n");
                    final ITextComponent comp = ComponentArgument.textComponent().parse(new StringReader(jsonComp));
                    p.sendMessage(comp, DiscordIntegration.dummyUUID);
                    if (ping.getKey()) {
                        if (LinkManager.isPlayerLinked(p.getUUID()) && LinkManager.getLink(null, p.getUUID()).settings.pingSound) {
                            p.connection.send(new SPlaySoundPacket(SoundEvents.NOTE_BLOCK_PLING.getLocation(), SoundCategory.MASTER, new Vector3d(p.position().x,p.position().y,p.position().z), 1,1));
                        }
                    }
                }
            }
            //Send to server console too
            final String jsonComp = GsonComponentSerializer.gson().serialize(msg).replace("\\\\n", "\n");
            final ITextComponent comp = ComponentArgument.textComponent().parse(new StringReader(jsonComp));
            ServerLifecycleHooks.getCurrentServer().sendMessage(comp, DiscordIntegration.dummyUUID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendIngameReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, EmojiUnion reactionEmote) {
        final List<ServerPlayerEntity> l = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        for (final ServerPlayerEntity p : l) {
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
        if (s.hasPermissions(4)) {
            try {
                ServerLifecycleHooks.getCurrentServer().getCommands().getDispatcher().execute(cmd.trim(), s.createCommandSourceStack());
            } catch (CommandSyntaxException e) {
                s.sendMessage(new StringTextComponent(e.getMessage()), DiscordIntegration.dummyUUID);
            }
        } else
            s.sendMessage(new StringTextComponent("Sorry, but the bot has no permissions...\nAdd this into the servers ops.json:\n```json\n {\n   \"uuid\": \"" + Configuration.instance().commands.senderUUID + "\",\n   \"name\": \"DiscordFakeUser\",\n   \"level\": 4,\n   \"bypassesPlayerLimit\": false\n }\n```"), DiscordIntegration.dummyUUID);

    }

    @Override
    public HashMap<UUID, String> getPlayers() {
        final HashMap<UUID, String> players = new HashMap<>();
        for (ServerPlayerEntity p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            players.put(p.getUUID(), p.getDisplayName().getString().isEmpty() ? p.getName().getString() : p.getDisplayName().getString());
        }
        return players;
    }

    @Override
    public void sendIngameMessage(String msg, UUID player) {
        final ServerPlayerEntity p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(player);
        if (p != null)
            p.sendMessage(new StringTextComponent(msg), DiscordIntegration.dummyUUID);

    }

    @Override
    public boolean isOnlineMode() {
        return Configuration.instance().bungee.isBehindBungee || ServerLifecycleHooks.getCurrentServer().usesAuthentication();
    }

    private void sendReactionMCMessage(ServerPlayerEntity target, Component msgComp) {
        final String jsonComp = GsonComponentSerializer.gson().serialize(msgComp).replace("\\\\n", "\n");
        try {
            final ITextComponent comp = ComponentArgument.textComponent().parse(new StringReader(jsonComp));
            target.sendMessage(comp, DiscordIntegration.dummyUUID);
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
        return false;
    }

    @Override
    public boolean playerHasPermissions(UUID player, MinecraftPermission... permissions) {
        return McServerInterface.super.playerHasPermissions(player, permissions);
    }
}
