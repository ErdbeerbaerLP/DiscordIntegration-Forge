package de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.concurrent.atomic.AtomicBoolean;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class LinkCommand extends DMCommand {

    @Override
    public String getName() {
        return "link";
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public String getDescription() {
        return "Links your Discord account with your Minecraft account";
    }

    @Override
    public void execute(String[] args, MessageReceivedEvent ev) {
        if (discord_instance.getChannel().getGuild().isMember(ev.getAuthor())) {
            Member m = discord_instance.getChannel().getGuild().getMember(ev.getAuthor());
            if (Configuration.instance().linking.requiredRoles.length != 0) {
                AtomicBoolean ok = new AtomicBoolean(false);
                m.getRoles().forEach((role) -> {
                    for (String s : Configuration.instance().linking.requiredRoles) {
                        if (s.equals(role.getId())) ok.set(true);
                    }
                });
                if (!ok.get()) {
                    ev.getChannel().sendMessage(Configuration.instance().localization.link_requiredRole).queue();
                    return;
                }
            }
        } else {
            ev.getChannel().sendMessage(Configuration.instance().localization.link_notMember).queue();
            return;
        }
        if (args.length > 1) {
            ev.getChannel().sendMessage(Configuration.instance().localization.tooManyArguments).queue();
            return;
        }
        if (args.length < 1) {
            ev.getChannel().sendMessage(Configuration.instance().localization.notEnoughArguments).queue();
            return;
        }
        if (!args[0].startsWith(Configuration.instance().commands.prefix))
            try {
                int num = Integer.parseInt(args[0]);
                if (PlayerLinkController.isDiscordLinked(ev.getAuthor().getId())) {
                    ev.getChannel().sendMessage(Configuration.instance().localization.alreadyLinked.replace("%player%", PlayerLinkController.getNameFromUUID(PlayerLinkController.getPlayerFromDiscord(ev.getAuthor().getId())))).queue();
                    return;
                }
                if (discord_instance.pendingLinks.containsKey(num)) {
                    final boolean linked = PlayerLinkController.linkPlayer(ev.getAuthor().getId(), discord_instance.pendingLinks.get(num).getValue());
                    if (linked) {
                        ev.getChannel().sendMessage(Configuration.instance().localization.linkSuccessful.replace("%name%", PlayerLinkController.getNameFromUUID(PlayerLinkController.getPlayerFromDiscord(ev.getAuthor().getId())))).queue();
                        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(discord_instance.pendingLinks.get(num).getValue()).sendMessage(new StringTextComponent("Your account is now linked with " + ev.getAuthor().getAsTag()), Util.DUMMY_UUID);
                    } else
                        ev.getChannel().sendMessage(Configuration.instance().localization.linkFailed).queue();
                } else {
                    ev.getChannel().sendMessage(Configuration.instance().localization.invalidLinkNumber).queue();
                }
            } catch (NumberFormatException nfe) {
                ev.getChannel().sendMessage(Configuration.instance().localization.linkNumberNAN).queue();
            }
    }

    @Override
    public boolean requiresLink() {
        return false;
    }
}
