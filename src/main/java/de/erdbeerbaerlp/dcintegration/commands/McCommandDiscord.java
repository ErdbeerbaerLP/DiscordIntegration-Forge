package de.erdbeerbaerlp.dcintegration.commands;

import de.erdbeerbaerlp.dcintegration.Configuration;
import de.erdbeerbaerlp.dcintegration.DiscordIntegration;
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
                            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Config reloaded " + (DiscordIntegration.discord_instance
                                    .restart() ? "and discord bot properly restarted" : (TextFormatting.RED + "but failed to properly restart the discord bot")) + "!"));
                        }).start();
                        return;
                    case "restart":
                        new Thread(() -> {
                            if (DiscordIntegration.discord_instance.restart()) {
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
                            new TextComponentString(DiscordIntegration.discord_instance.togglePlayerIgnore((EntityPlayer) sender) ? Configuration.DISCORD_COMMAND.IGNORECMD_UNIGNORE : Configuration.DISCORD_COMMAND.IGNORECMD_IGNORE));
                    return;
                default:
                    break;
            }
        }
        sender.sendMessage(new TextComponentString(Configuration.DISCORD_COMMAND.MESSAGE).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Configuration.DISCORD_COMMAND.HOVER)))
                                                                                                              .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Configuration.DISCORD_COMMAND.URL))));
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
