package de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class SettingsCommand extends DMCommand {

    @Override
    public String getName() {
        return "settings";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"set"};
    }

    @Override
    public String getDescription() {
        return Configuration.instance().localization.commands.descriptions.settings;
    }

    @Override
    public void execute(String[] args, MessageReceivedEvent ev) {
        if (!PlayerLinkController.isDiscordLinked(ev.getAuthor().getId())) {
            ev.getChannel().sendMessage(Configuration.instance().localization.linking.notLinked.replace("%method%", Configuration.instance().linking.whitelistMode ? (Configuration.instance().localization.linking.linkMethodWhitelist.replace("%prefix%", Configuration.instance().commands.prefix)) : Configuration.instance().localization.linking.linkMethodIngame)).queue();
            return;
        }
        if (args.length == 2 && args[0].equals("get")) {
            if (discord_instance.getSettings().containsKey(args[1])) {
                final PlayerSettings settings = PlayerLinkController.getSettings(ev.getAuthor().getId(), null);
                try {
                    ev.getChannel().sendMessage(Configuration.instance().localization.personalSettings.personalSettingGet.replace("%bool%", settings.getClass().getField(args[1]).getBoolean(settings) ? "true" : "false")).queue();
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            } else
                ev.getChannel().sendMessage(Configuration.instance().localization.personalSettings.invalidPersonalSettingKey.replace("%key%", args[1])).queue();
        } else if (args.length == 3 && args[0].equals("set")) {
            if (discord_instance.getSettings().containsKey(args[1])) {
                final PlayerSettings settings = PlayerLinkController.getSettings(ev.getAuthor().getId(), null);
                boolean newval;
                try {
                    newval = Boolean.parseBoolean(args[2]);
                } catch (NumberFormatException e) {
                    newval = false;
                }
                final boolean newValue = newval;
                try {
                    settings.getClass().getDeclaredField(args[1]).set(settings, newValue);
                    PlayerLinkController.updatePlayerSettings(ev.getAuthor().getId(), null, settings);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                    ev.getChannel().sendMessage(Configuration.instance().localization.personalSettings.settingUpdateFailed).queue();
                }
                ev.getChannel().sendMessage(Configuration.instance().localization.personalSettings.settingUpdateSuccessful).queue();
            } else
                ev.getChannel().sendMessage(Configuration.instance().localization.personalSettings.invalidPersonalSettingKey.replace("%key%", args[1])).queue();
        } else if (args.length == 1) {
            final MessageBuilder msg = new MessageBuilder(Configuration.instance().localization.personalSettings.settingsCommandUsage.replace("%prefix%", Configuration.instance().commands.prefix));
            final EmbedBuilder b = new EmbedBuilder();
            final PlayerSettings settings = PlayerLinkController.getSettings(ev.getAuthor().getId(), null);
            discord_instance.getSettings().forEach((name, desc) -> {
                if (!(!Configuration.instance().webhook.enable && name.equals("useDiscordNameInChannel"))) {
                    try {
                        b.addField(name + " == " + (((boolean) settings.getClass().getDeclaredField(name).get(settings)) ? "true" : "false"), desc, false);
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        b.addField(name + " == Unknown", desc, false);
                    }
                }
            });
            b.setAuthor(Configuration.instance().localization.personalSettings.personalSettingsHeader);
            msg.setEmbed(b.build());
            ev.getChannel().sendMessage(msg.build()).queue();
        } else {
            ev.getChannel().sendMessage(Configuration.instance().localization.personalSettings.settingsCommandUsage.replace("%prefix%", Configuration.instance().commands.prefix)).queue();
        }
    }
}
