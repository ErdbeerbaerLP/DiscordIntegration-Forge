package de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class SettingsCommand extends DMCommand {
    private final String cmdUsages = "Usages:\n\n" + Configuration.instance().commands.prefix + "settings - lists all available keys\n" + Configuration.instance().commands.prefix + "settings get <key> - Gets the current settings value\n" + Configuration.instance().commands.prefix + "settings set <key> <value> - Sets an Settings value";

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
        return "Allows you to edit your personal settings";
    }

    @Override
    public void execute(String[] args, MessageReceivedEvent ev) {
        if (!PlayerLinkController.isDiscordLinked(ev.getAuthor().getId()))
            ev.getChannel().sendMessage(Configuration.instance().localization.notLinked.replace("%method%", Configuration.instance().linking.whitelistMode ? (Configuration.instance().localization.linkMethodWhitelist.replace("%prefix%", Configuration.instance().commands.prefix)) : Configuration.instance().localization.linkMethodIngame)).queue();
        else if (args.length == 0) {
            final MessageBuilder mb = new MessageBuilder();
            mb.setContent(cmdUsages);
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
            b.setAuthor(Configuration.instance().localization.personalSettingsHeader);
            mb.setEmbed(b.build());
            ev.getChannel().sendMessage(mb.build()).queue();
        } else if (args.length == 2 && args[0].equals("get")) {
            if (discord_instance.getSettings().containsKey(args[1])) {
                final PlayerSettings settings = PlayerLinkController.getSettings(ev.getAuthor().getId(), null);
                try {
                    ev.getChannel().sendMessage(Configuration.instance().localization.personalSettingGet.replace("%bool%", settings.getClass().getField(args[1]).getBoolean(settings) ? "true" : "false")).queue();
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            } else
                ev.getChannel().sendMessage(Configuration.instance().localization.invalidPersonalSettingKey.replace("%key%", args[1])).queue();
        } else if (args.length == 3 && args[0].equals("set")) {
            if (discord_instance.getSettings().containsKey(args[1])) {
                final PlayerSettings settings = PlayerLinkController.getSettings(ev.getAuthor().getId(), null);
                int newval;
                try {
                    newval = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    newval = -1;
                }
                final boolean newValue = newval == -1 ? Boolean.parseBoolean(args[2]) : newval >= 1;
                try {
                    settings.getClass().getDeclaredField(args[1]).set(settings, newValue);
                    PlayerLinkController.updatePlayerSettings(ev.getAuthor().getId(), null, settings);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                    ev.getChannel().sendMessage(Configuration.instance().localization.settingUpdateFailed).queue();
                }
                ev.getChannel().sendMessage(Configuration.instance().localization.settingUpdateSuccessful).queue();
            } else
                ev.getChannel().sendMessage(Configuration.instance().localization.invalidPersonalSettingKey.replace("%key%", args[1])).queue();
        } else {
            ev.getChannel().sendMessage(cmdUsages).queue();
        }
    }
}
