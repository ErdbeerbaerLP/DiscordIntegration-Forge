package de.erdbeerbaerlp.dcintegration.forge.command;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@SuppressWarnings("EntityConstructor")
public class DCCommandSender extends FakePlayer {

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(DCCommandSender.class.getSimpleName()).setDaemon(true).build());
    private static final UUID uuid = UUID.fromString(Configuration.instance().commands.senderUUID);
    private final CompletableFuture<InteractionHook> cmdMsg;
    private CompletableFuture<Message> cmdMessage;
    final StringBuilder message = new StringBuilder();

    public DCCommandSender(User user, CompletableFuture<InteractionHook> cmdMsg) {
        super(FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0], new GameProfile(uuid, "@" + user.getName() + "#" + user.getDiscriminator()));
        this.cmdMsg = cmdMsg;
    }



    private static String textComponentToDiscordMessage(ITextComponent component) {
        return MessageUtils.removeFormatting(component.getUnformattedText());

    }

    @Override
    public boolean canUseCommand(int i, String s) {
        return true;
    }

    @Override
    public void sendMessage(ITextComponent component) {
        Preconditions.checkNotNull(component);
        message.append(textComponentToDiscordMessage(component)).append("\n");
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
    public void sendStatusMessage(ITextComponent component, boolean actionBar) {
        Preconditions.checkNotNull(component);
        message.append(textComponentToDiscordMessage(component)).append("\n");
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