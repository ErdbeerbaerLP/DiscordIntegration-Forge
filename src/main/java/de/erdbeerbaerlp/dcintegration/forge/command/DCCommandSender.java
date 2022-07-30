package de.erdbeerbaerlp.dcintegration.forge.command;

import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingPlayerChatMessage;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class DCCommandSender extends FakePlayer {

    private static final UUID uuid = UUID.fromString(Configuration.instance().commands.senderUUID);
    private final CompletableFuture<InteractionHook> cmdMsg;
    private CompletableFuture<Message> cmdMessage;
    final StringBuilder message = new StringBuilder();

    public DCCommandSender(CompletableFuture<InteractionHook> cmdMsg, User user) {
        super(ServerLifecycleHooks.getCurrentServer().overworld(), new GameProfile(uuid, "@" + user.getAsTag()));
        this.cmdMsg = cmdMsg;
    }


    private static String textComponentToDiscordMessage(Component component) {
        if (component == null) return "";
        return MessageUtils.convertMCToMarkdown(component.getString());
    }

    @Override
    public void sendSystemMessage(Component textComponent, boolean unused) {
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
    public void sendChatMessage(OutgoingPlayerChatMessage p_243284_, boolean p_243285_, ChatType.Bound p_243314_) {
        message.append(textComponentToDiscordMessage(p_243284_.serverContent())).append("\n");
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
    public void sendSystemMessage(Component textComponent) {
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

}