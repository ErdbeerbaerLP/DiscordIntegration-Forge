package de.erdbeerbaerlp.dcintegration.forge.command;

import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
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
        super(ServerLifecycleHooks.getCurrentServer().overworld(), new GameProfile(uuid, "@" + (!user.getDiscriminator().equals("0000") ? user.getAsTag() : user.getName())));
        this.cmdMsg = cmdMsg;
    }


    private static String textComponentToDiscordMessage(ITextComponent component) {
        if (component == null) return "";
        return MessageUtils.convertMCToMarkdown(component.getString());
    }

    @Override
    public void sendMessage(ITextComponent p_215097_, UUID uuid) {
        message.append(textComponentToDiscordMessage(p_215097_)).append("\n");
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
    public void displayClientMessage(ITextComponent chatComponent, boolean actionBar) {
        message.append(textComponentToDiscordMessage(chatComponent)).append("\n");
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