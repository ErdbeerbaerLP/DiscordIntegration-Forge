package de.erdbeerbaerlp.dcintegration.forge.command;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class DCCommandSender implements ICommandSource {
    private final CompletableFuture<InteractionHook> cmdMsg;
    private final ITextComponent name;
    private CompletableFuture<Message> cmdMessage;
    final StringBuilder message = new StringBuilder();

    private final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

    public DCCommandSender(CompletableFuture<InteractionHook> cmdMsg, User user) {
        final Member member = DiscordIntegration.INSTANCE.getMemberById(user.getId());
        if (member != null)
            name = new StringTextComponent("@" + (!member.getUser().getDiscriminator().equals("0000") ? member.getUser().getAsTag() : member.getEffectiveName()))
                    .setStyle(Style.EMPTY.withHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new StringTextComponent(Localization.instance().discordUserHover
                                            .replace("%user#tag%", !member.getUser().getDiscriminator().equals("0000") ? member.getUser().getAsTag() : member.getEffectiveName())
                                            .replace("%user%", member.getEffectiveName())
                                            .replace("%id%", member.getId())))));
        else
            name = new StringTextComponent("@" + (!user.getDiscriminator().equals("0000") ? user.getAsTag() : user.getEffectiveName()))
                    .setStyle(Style.EMPTY.withHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new StringTextComponent(Localization.instance().discordUserHover
                                            .replace("%user#tag%", !user.getDiscriminator().equals("0000") ? user.getAsTag() : user.getEffectiveName())
                                            .replace("%user%", user.getEffectiveName())
                                            .replace("%id%", user.getId())))));

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
            cmdMessage.thenAccept((msg) -> {
                cmdMessage = msg.editMessage(message.toString().trim()).submit();
            });
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return true;
    }

    public CommandSource createCommandSourceStack() {
        ServerWorld serverworld = this.server.overworld();
        return new CommandSource(this, Vector3d.atLowerCornerOf(serverworld.getSharedSpawnPos()), Vector2f.ZERO, serverworld, 4, "Rcon", name, this.server, (Entity) null);
    }
}