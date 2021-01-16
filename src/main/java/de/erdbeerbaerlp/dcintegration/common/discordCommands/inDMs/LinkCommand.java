package de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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
        return Configuration.instance().localization.commands.descriptions.link;
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
                    ev.getChannel().sendMessage(Configuration.instance().localization.linking.link_requiredRole).queue();
                    return;
                }
            }
        } else {
            ev.getChannel().sendMessage(Configuration.instance().localization.linking.link_notMember).queue();
            return;
        }
        if (args.length > 1) {
            ev.getChannel().sendMessage(Configuration.instance().localization.commands.tooManyArguments).queue();
            return;
        }
        if (args.length < 1) {
            ev.getChannel().sendMessage(Configuration.instance().localization.commands.notEnoughArguments).queue();
            return;
        }
        if (!args[0].startsWith(Configuration.instance().commands.prefix))
            try {
                int num = Integer.parseInt(args[0]);
                if (PlayerLinkController.isDiscordLinked(ev.getAuthor().getId()) && (!discord_instance.pendingBedrockLinks.isEmpty() && PlayerLinkController.isDiscordLinkedBedrock(ev.getAuthor().getId()))) {
                    ev.getChannel().sendMessage(Configuration.instance().localization.linking.alreadyLinked.replace("%player%", MessageUtils.getNameFromUUID(PlayerLinkController.getPlayerFromDiscord(ev.getAuthor().getId())))).queue();
                    return;
                }
                if (discord_instance.pendingLinks.containsKey(num)) {
                    final boolean linked = PlayerLinkController.linkPlayer(ev.getAuthor().getId(), discord_instance.pendingLinks.get(num).getValue());
                    if (linked) {
                        ev.getChannel().sendMessage(Configuration.instance().localization.linking.linkSuccessful.replace("%prefix%", Configuration.instance().commands.prefix).replace("%player%", MessageUtils.getNameFromUUID(PlayerLinkController.getPlayerFromDiscord(ev.getAuthor().getId())))).queue();
                        discord_instance.srv.sendMCMessage(Configuration.instance().localization.linking.linkSuccessfulIngame.replace("%name%", ev.getAuthor().getName()).replace("%name#tag%", ev.getAuthor().getAsTag()), discord_instance.pendingLinks.get(num).getValue());
                    } else
                        ev.getChannel().sendMessage(Configuration.instance().localization.linking.linkFailed).queue();
                } else if(discord_instance.pendingBedrockLinks.containsKey(num)){
                    final boolean linked = PlayerLinkController.linkBedrockPlayer(ev.getAuthor().getId(), discord_instance.pendingBedrockLinks.get(num).getValue());
                    if (linked) {
                        ev.getChannel().sendMessage(Configuration.instance().localization.linking.linkSuccessful.replace("%prefix%", Configuration.instance().commands.prefix).replace("%player%", MessageUtils.getNameFromUUID(PlayerLinkController.getBedrockPlayerFromDiscord(ev.getAuthor().getId())))).queue();
                        discord_instance.srv.sendMCMessage(Configuration.instance().localization.linking.linkSuccessfulIngame.replace("%name%", ev.getAuthor().getName()).replace("%name#tag%", ev.getAuthor().getAsTag()), discord_instance.pendingBedrockLinks.get(num).getValue());
                    } else
                        ev.getChannel().sendMessage(Configuration.instance().localization.linking.linkFailed).queue();
                } else {
                    ev.getChannel().sendMessage(Configuration.instance().localization.linking.invalidLinkNumber).queue();
                }
            } catch (NumberFormatException nfe) {
                ev.getChannel().sendMessage(Configuration.instance().localization.linking.linkNumberNAN).queue();
            }
    }

    @Override
    public boolean canUserExecuteCommand(User user) {
        return true;
    }
}
