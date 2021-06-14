package de.erdbeerbaerlp.dcintegration.forge.command;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.entities.User;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@SuppressWarnings("EntityConstructor")
public class DCCommandSender extends FakePlayer {

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(DCCommandSender.class.getSimpleName()).setDaemon(true).build());
    private static final UUID uuid = UUID.fromString(Configuration.instance().commands.senderUUID);
    private String channelID;

    public DCCommandSender(User user, String channel) {
        super(FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0], new GameProfile(uuid, "@" + user.getName() + "#" + user.getDiscriminator()));
        this.channelID = channel;
    }

    @SuppressWarnings("unused")
    public DCCommandSender(WorldServer world, String name, String channel) {
        super(world, new GameProfile(uuid, "@" + name));
        this.channelID = channel;
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
        Variables.discord_instance.sendMessageFuture(textComponentToDiscordMessage(component), channelID);
    }

    @Override
    public void sendStatusMessage(ITextComponent component, boolean actionBar) {
        Preconditions.checkNotNull(component);
        Variables.discord_instance.sendMessageFuture(textComponentToDiscordMessage(component), channelID);
    }
}