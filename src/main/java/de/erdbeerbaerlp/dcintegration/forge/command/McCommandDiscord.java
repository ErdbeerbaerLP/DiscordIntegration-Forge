package de.erdbeerbaerlp.dcintegration.forge.command;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.forge.DiscordIntegration;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.List;


public class McCommandDiscord implements ICommand
{
    private final ArrayList<String> opTabComps = new ArrayList<>();
    private final ArrayList<String> normalTabComps = new ArrayList<>();
    private final ArrayList<String> aliases = new ArrayList<>();

    public McCommandDiscord() {
        aliases.add("dc");
        aliases.add("disc");
        normalTabComps.add("ignore");

        opTabComps.add("reload");
        opTabComps.add("restart");
        opTabComps.addAll(normalTabComps);
    }

    @Override
    public String getName() {
        return "discord";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/discord";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length > 0 && sender instanceof EntityPlayer) {
            if (sender.canUseCommand(4, "discord")) {
                switch (args[0]) {
                    case "reload":
                        new Thread(() -> {
                            ConfigManager.sync(DiscordIntegration.MODID, Config.Type.INSTANCE);
                            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Config reloaded " + (Variables.discord_instance
                                    .restart() ? "and discord bot properly restarted" : (TextFormatting.RED + "but failed to properly restart the discord bot")) + "!"));
                        }).start();
                        return;
                    case "restart":
                        new Thread(() -> {
                            if (Variables.discord_instance.restart()) {
                                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Discord bot restarted!"));
                            }
                            else sender.sendMessage(new TextComponentString(TextFormatting.RED + "Failed to properly restart the discord bot!"));
                        }).start();
                        return;
                    default:
                        break;
                }
            }
            switch (args[0]) {
                case "ignore":
                    sender.sendMessage(
                            new TextComponentString(Variables.discord_instance.togglePlayerIgnore(((EntityPlayer) sender).getUniqueID()) ? Localization.instance().commands.commandIgnore_unignore : Localization.instance().commands.commandIgnore_ignore));
                    return;
                case "link":
                    if (Configuration.instance().linking.enableLinking && FMLCommonHandler.instance().getMinecraftServerInstance().isServerInOnlineMode() && !Configuration.instance().linking.whitelistMode) {
                        if (PlayerLinkController.isPlayerLinked(sender.getCommandSenderEntity().getUniqueID())) {
                            sender.sendMessage(new TextComponentString(TextFormatting.RED +Localization.instance().linking.alreadyLinked.replace("%player%", Variables.discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromBedrockPlayer(sender.getCommandSenderEntity().getUniqueID())).getAsTag())));
                           break;
                        }
                        final int r = Variables.discord_instance.genLinkNumber(sender.getCommandSenderEntity().getUniqueID());
                        sender.sendMessage(new TextComponentString(Localization.instance().linking.linkMsgIngame.replace("%num%", r + "")).setStyle(new Style().setColor(TextFormatting.AQUA).setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/link " + r)).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Localization.instance().linking.hoverMsg_copyClipboard)))));
                    } else {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED +Localization.instance().commands.subcommandDisabled));
                    }
                    break;
                default:
                    break;
            }
        }
        sender.sendMessage(new TextComponentString(Configuration.instance().ingameCommand.message).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Configuration.instance().ingameCommand.hoverMessage)))
                .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Configuration.instance().ingameCommand.inviteURL))));
    }

    @Override
    public int compareTo(ICommand o) {
        return 0;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {

        return sender.canUseCommand(4, "discord") ? opTabComps : normalTabComps;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

}