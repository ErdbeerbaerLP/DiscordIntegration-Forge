package de.erdbeerbaerlp.dcintegration.forge.command;

import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class DCCommandSender extends FakePlayer {

    private static final UUID uuid = UUID.fromString(Configuration.instance().commands.senderUUID);
    private final CompletableFuture<InteractionHook> cmdMsg;
    private CompletableFuture<Message> cmdMessage;
    final StringBuilder message = new StringBuilder();

    public DCCommandSender(CompletableFuture<InteractionHook> cmdMsg, User user) {
        super(ServerLifecycleHooks.getCurrentServer().func_241755_D_(), new GameProfile(uuid, "@" + user.getAsTag()));
        this.cmdMsg = cmdMsg;
    }


    private static String textComponentToDiscordMessage(ITextComponent component) {
        if (component == null) return "";
        return MessageUtils.convertMCToMarkdown(component.getString());
    }

    @Override
    public void sendMessage(ITextComponent textComponent, UUID uuid) {
        message.append(textComponentToDiscordMessage(textComponent)).append("\n");
        if (cmdMessage == null)
            cmdMsg.thenAccept((msg) -> {
                cmdMessage = msg.editOriginal(message.toString().trim()).submit();
            });
        else
            cmdMessage.thenAccept((msg)->{
                cmdMessage = msg.editMessage(message.toString().trim()).submit();
            });
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
        sendMessage(component, Util.DUMMY_UUID);
    }
}