package de.erdbeerbaerlp.dcintegration.commands;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.dcintegration.Configuration;
import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
import net.dv8tion.jda.core.entities.User;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SuppressWarnings("EntityConstructor")
public class DCCommandSender extends FakePlayer {

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat(DCCommandSender.class.getSimpleName())
                    .setDaemon(true)
                    .build()
    );
    private static final UUID uuid = UUID.fromString(Configuration.INSTANCE.senderUUID.get());
    private final CommandFromCFG command;

    public DCCommandSender(User user, CommandFromCFG command) {
        super(ServerLifecycleHooks.getCurrentServer().getWorld(DimensionType.OVERWORLD), new GameProfile(uuid, "@" + user.getName() + "#" + user.getDiscriminator()));
        this.command = command;
    }

    @SuppressWarnings("unused")
    public DCCommandSender(ServerWorld world, String name, CommandFromCFG command) {
        super(world, new GameProfile(uuid, "@" + name));
        this.command = command;
    }

    private static String textComponentToDiscordMessage(ITextComponent component) {
        return DiscordIntegration.removeFormatting(component.getUnformattedComponentText());

    }


    @Override
    public void sendMessage(ITextComponent component) {
        Preconditions.checkNotNull(component);
        DiscordIntegration.discord_instance.sendMessage(textComponentToDiscordMessage(component));
    }

    @Override
    public void sendStatusMessage(ITextComponent component, boolean actionBar) {
        Preconditions.checkNotNull(component);
        DiscordIntegration.discord_instance.sendMessage(textComponentToDiscordMessage(component));
    }
}