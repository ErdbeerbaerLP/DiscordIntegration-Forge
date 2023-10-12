package de.erdbeerbaerlp.dcintegration.forge.command;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class DCCommandSender implements CommandSource {
    private final CompletableFuture<InteractionHook> cmdMsg;
    private final Component name;
    private CompletableFuture<Message> cmdMessage;
    final StringBuilder message = new StringBuilder();

    private final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

    public DCCommandSender(CompletableFuture<InteractionHook> cmdMsg, User user) {
        final Member member = DiscordIntegration.INSTANCE.getMemberById(user.getId());
        if (member != null)
            name =  new TextComponent("@" + (!member.getUser().getDiscriminator().equals("0000") ? member.getUser().getAsTag() : member.getEffectiveName()))
                    .setStyle(Style.EMPTY.withHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new TextComponent(Localization.instance().discordUserHover
                                            .replace("%user#tag%", !member.getUser().getDiscriminator().equals("0000") ? member.getUser().getAsTag() : member.getEffectiveName())
                                            .replace("%user%", member.getEffectiveName())
                                            .replace("%id%", member.getId())))));
        else
            name = new TextComponent("@" + (!user.getDiscriminator().equals("0000") ? user.getAsTag() : user.getEffectiveName()))
                    .setStyle(Style.EMPTY.withHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new TextComponent(Localization.instance().discordUserHover
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
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    public CommandSourceStack createCommandSourceStack() {
        return new CommandSourceStack(this, Vec3.ZERO, new Vec2(0.0F, 0.0F), server.getLevel(ServerLevel.OVERWORLD), 4, this.name.getString(), this.name, this.server, (Entity) null);
    }

    @Override
    public boolean shouldInformAdmins() {
        return true;
    }


}