package de.erdbeerbaerlp.dcintegration.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.erdbeerbaerlp.dcintegration.Configuration;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class McCommandDiscord {
public class McCommandDiscord implements ICommand {
	private final ArrayList<String> opTabComps = new ArrayList<>();

	public McCommandDiscord() {
		opTabComps.add("reload");
		opTabComps.add("restart");
	}

	@Override
	public String getName() {
		return "discord";
	}

    public McCommandDiscord(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("discord").executes((ctx) -> {
                    ctx.getSource().sendFeedback(new StringTextComponent(Configuration.INSTANCE.dcCmdMsg.get()).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(Configuration.INSTANCE.dcCmdMsgHover.get()))).setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Configuration.INSTANCE.dcCmdURL.get()))), false);
                    return 0;
                })
        );
    }
	@Override
	public String getUsage(ICommandSender sender) {
		return "/discord";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		if (sender.canUseCommand(4, "discord") && args.length > 0) {
			switch (args[0]) {
				case "reload":
					new Thread(() -> {
						ConfigManager.sync(DiscordIntegration.MODID, Config.Type.INSTANCE);
						sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Config reloaded " + (DiscordIntegration.discord_instance.restart() ? "and discord bot properly restarted" : (TextFormatting.RED + "but failed to properly restart the discord bot")) + "!"));
					}).start();
					break;
				case "restart":
					new Thread(() -> {
						if (DiscordIntegration.discord_instance.restart()) {
							sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Discord bot restarted!"));
						} else
							sender.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to properly restart the discord bot!"));
					}).start();
					break;
				default:
					break;
			}
		} else
			sender.sendMessage(new

					TextComponentString(Configuration.DISCORD_COMMAND.MESSAGE).

					setStyle(new Style().

							setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Configuration.DISCORD_COMMAND.HOVER))).

							setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Configuration.DISCORD_COMMAND.URL))));
	}

	@Override
	public int compareTo(ICommand o) {
		return 0;
	}

	@Override
	public List<String> getAliases() {
		return new ArrayList<>();
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  BlockPos targetPos) {
		return sender.canUseCommand(4, "discord") ? opTabComps : new ArrayList<>();
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}

}
