package de.erdbeerbaerlp.dcintegration.forge.command;

import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.entities.User;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;

import java.util.UUID;


public class DCCommandSender extends FakePlayer {

    private static final UUID uuid = UUID.fromString(Configuration.instance().commands.senderUUID);
    private final String channelID;

    public DCCommandSender(User user, String channel) {
        super(ServerLifecycleHooks.getCurrentServer().overworld(), new GameProfile(uuid, "@" + user.getName() + "#" + user.getDiscriminator()));
        this.channelID = channel;
    }

    public DCCommandSender(ServerLevel world, String name, String channel) {
        super(world, new GameProfile(uuid, "@" + name));
        this.channelID = channel;
    }

    private static String textComponentToDiscordMessage(Component component) {
        if (component == null) return "";
        return MessageUtils.convertMCToMarkdown(component.getString());
    }

    @Override
    public void sendMessage(Component textComponent, UUID uuid) {
        Variables.discord_instance.sendMessageFuture(textComponentToDiscordMessage(textComponent), channelID);
    }

}