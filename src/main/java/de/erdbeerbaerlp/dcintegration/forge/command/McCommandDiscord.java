package de.erdbeerbaerlp.dcintegration.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.MCSubCommand;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.McCommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;


public class McCommandDiscord {
    public McCommandDiscord(CommandDispatcher<CommandSource> dispatcher) {
        final LiteralArgumentBuilder<CommandSource> l = Commands.literal("discord");
        if (Configuration.instance().ingameCommand.enabled) l.executes((ctx) -> {
            ctx.getSource().sendSuccess(TextComponentUtils.mergeStyles(new StringTextComponent(Configuration.instance().ingameCommand.message),
                            Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(Configuration.instance().ingameCommand.hoverMessage)))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Configuration.instance().ingameCommand.inviteURL))), false);
            return 0;
        });
        for (MCSubCommand cmd : McCommandRegistry.getCommands()) {
            l.then(Commands.literal(cmd.getName()));
        }
        dispatcher.register(l);
    }
}
