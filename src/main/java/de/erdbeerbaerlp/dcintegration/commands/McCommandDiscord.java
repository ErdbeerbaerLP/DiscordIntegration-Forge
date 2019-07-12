package de.erdbeerbaerlp.dcintegration.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.erdbeerbaerlp.dcintegration.Configuration;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

public class McCommandDiscord {

    public McCommandDiscord(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("discord").executes((ctx) -> {
                    ctx.getSource().sendFeedback(new StringTextComponent(Configuration.INSTANCE.dcCmdMsg.get()).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(Configuration.INSTANCE.dcCmdMsgHover.get()))).setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Configuration.INSTANCE.dcCmdURL.get()))), false);
                    return 0;
                })
        );
    }
}
