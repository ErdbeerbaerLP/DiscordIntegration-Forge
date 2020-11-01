package de.erdbeerbaerlp.dcintegration.forge.command;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat.CommandFromCFG;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.entities.User;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.UUID;


public class DCCommandSender extends FakePlayer {

    private static final UUID uuid = UUID.fromString(Configuration.instance().commands.senderUUID);
    private final CommandFromCFG command;
    private final String channelID;

    public DCCommandSender(User user, CommandFromCFG command, String channel) {
        super(ServerLifecycleHooks.getCurrentServer().getWorld(World.OVERWORLD), new GameProfile(uuid, "@" + user.getName() + "#" + user.getDiscriminator()));
        this.command = command;
        this.channelID = channel;
    }

    public DCCommandSender(ServerWorld world, String name, CommandFromCFG command, String channel) {
        super(world, new GameProfile(uuid, "@" + name));
        this.command = command;
        this.channelID = channel;
    }

    private static String textComponentToDiscordMessage(ITextComponent component) {
        if (component == null) return "";
        return MessageUtils.convertMCToMarkdown(component.getString());
    }

    @Override
    public void sendMessage(ITextComponent textComponent, UUID uuid) {
        Variables.discord_instance.sendMessageFuture(textComponentToDiscordMessage(textComponent), channelID);
    }

    @Override
    public boolean shouldReceiveFeedback() {
        return true;
    }

    @Override
    public boolean shouldReceiveErrors() {
        return true;
    }


    @Override
    public void sendStatusMessage(ITextComponent component, boolean actionBar) {
        Preconditions.checkNotNull(component);
        Variables.discord_instance.sendMessageFuture(textComponentToDiscordMessage(component), channelID);
    }
}