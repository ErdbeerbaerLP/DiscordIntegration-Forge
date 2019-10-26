package de.erdbeerbaerlp.dcintegration.commands;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.Configuration;
import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import net.dv8tion.jda.api.entities.User;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@SuppressWarnings("EntityConstructor")
public class DCCommandSender extends FakePlayer
{
    
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(DCCommandSender.class.getSimpleName()).setDaemon(true).build());
    private static final UUID uuid = UUID.fromString(Configuration.COMMANDS.SENDER_UUID);
    private final CommandFromCFG command;
    
    public DCCommandSender(User user, CommandFromCFG command) {
        super(FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0], new GameProfile(uuid, "@" + user.getName() + "#" + user.getDiscriminator()));
        this.command = command;
    }
    
    @SuppressWarnings("unused")
    public DCCommandSender(WorldServer world, String name, CommandFromCFG command) {
        super(world, new GameProfile(uuid, "@" + name));
        this.command = command;
    }
    
    private static String textComponentToDiscordMessage(ITextComponent component) {
        return DiscordIntegration.removeFormatting(component.getUnformattedText());
        
    }
    
    @Override
    public boolean canUseCommand(int i, String s) {
        return true;
    }
    
    @Override
    public void sendMessage(ITextComponent component) {
        Preconditions.checkNotNull(component);
        DiscordIntegration.discord_instance.sendMessageFuture(textComponentToDiscordMessage(component));
    }
    
    @Override
    public void sendStatusMessage(ITextComponent component, boolean actionBar) {
        Preconditions.checkNotNull(component);
        DiscordIntegration.discord_instance.sendMessageFuture(textComponentToDiscordMessage(component));
    }
}
