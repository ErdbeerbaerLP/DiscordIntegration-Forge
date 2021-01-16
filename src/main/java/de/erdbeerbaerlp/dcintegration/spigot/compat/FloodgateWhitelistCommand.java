package de.erdbeerbaerlp.dcintegration.spigot.compat;

import de.erdbeerbaerlp.dcintegration.common.Discord;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs.DMCommand;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

public class FloodgateWhitelistCommand extends DMCommand {
    @Override
    public String getName() {
        return "bwhitelist";
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public String getDescription() {
        return "Whitelists your bedrock account";
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
        if (PlayerLinkController.isDiscordLinkedBedrock(ev.getAuthor().getId())) {
            ev.getChannel().sendMessage(Configuration.instance().localization.linking.alreadyLinked.replace("%player%", MessageUtils.getNameFromUUID(PlayerLinkController.getPlayerFromDiscord(ev.getAuthor().getId())))).queue();
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
        UUID u;
        String name = args[0];
        try {
            try {
                final URL url = new URL("https://floodgate-uuid.heathmitchell1.repl.co/uuid?gamertag=" + name);
                final HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                final BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String sr = r.readLine();
                if (sr.startsWith("The UUID of")) {
                    sr = sr.replace("The UUID of "+name+" is ", "");
                    u = UUID.fromString(sr);
                } else
                    u = Discord.dummyUUID;
            } catch (IOException ex) {
                u = Discord.dummyUUID;
                ex.printStackTrace();
            }
            final boolean linked = PlayerLinkController.linkBedrockPlayer(ev.getAuthor().getId(), u);
            if (linked)
                ev.getChannel().sendMessage(Configuration.instance().localization.linking.linkSuccessful.replace("%prefix%", Configuration.instance().commands.prefix).replace("%player%", name)).queue();
            else
                ev.getChannel().sendMessage(Configuration.instance().localization.linking.linkFailed).queue();
        } catch (IllegalArgumentException e) {
            ev.getChannel().sendMessage(Configuration.instance().localization.linking.link_argumentNotUUID.replace("%prefix%", Configuration.instance().commands.prefix).replace("%arg%", name)).queue();
        }
    }

    @Override
    public boolean canUserExecuteCommand(User user) {
        if(user == null) return false;
        return !PlayerLinkController.isDiscordLinkedBedrock(user.getId());
    }
}
