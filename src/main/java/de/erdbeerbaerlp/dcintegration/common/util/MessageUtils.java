package de.erdbeerbaerlp.dcintegration.common.util;

import com.google.gson.JsonArray;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerSettings;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

public class MessageUtils {


    static final Pattern URL_PATTERN = Pattern.compile(
            //         schema                          ipv4            OR        namespace                 port     path         ends
            //   |-----------------|        |-------------------------|  |-------------------------|    |---------| |--|   |---------------|
            "((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"\u00A7 \n]|$))",
            Pattern.CASE_INSENSITIVE);

    public static String[] makeStringArray(final JsonArray channelID) {
        final String[] out = new String[channelID.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = channelID.get(i).getAsString();
        }
        return out;
    }

    public static String getFullUptime() {
        if (Variables.started == 0) {
            return "?????";
        }
        final Duration duration = Duration.between(Instant.ofEpochMilli(Variables.started), Instant.now());
        return DurationFormatUtils.formatDuration(duration.toMillis(), Configuration.instance().localization.uptimeFormat);
    }

    public static int getUptimeSeconds() {
        long diff = new Date().getTime() - Variables.started;
        return (int) Math.floorDiv(diff, 1000);
    }

    public static int getUptimeMinutes() {
        return Math.floorDiv(getUptimeSeconds(), 60);
    }

    public static int getUptimeHours() {
        return Math.floorDiv(getUptimeMinutes(), 60);
    }

    public static int getUptimeDays() {
        return Math.floorDiv(getUptimeHours(), 24);
    }

    public static String getDiscordName(final UUID p) {
        if (Variables.discord_instance == null) return null;
        if (Configuration.instance().linking.enableLinking && PlayerLinkController.isPlayerLinked(p)) {
            final PlayerSettings settings = PlayerLinkController.getSettings(null, p);
            if (settings != null && settings.useDiscordNameInChannel) {
                return Variables.discord_instance.getChannel().getGuild().getMember(Variables.discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromPlayer(p))).getEffectiveName();
            }
        }
        return null;
    }

    public static String escapeMarkdown(String in) {
        return in.replace("(?<!\\\\)[`*_|~]/g", "\\\\$0");
    }

    public static String escapeMarkdownCodeBlocks(String in) {
        return in.replace("(?<!\\\\)`/g", "\\\\$0");
    }

    public static String convertMarkdownToMCFormattingOld(String in) {
        if (!Configuration.instance().messages.convertCodes) return in;
        in = escapeMarkdownCodeBlocks(in);
        try {
            return LegacyComponentSerializer.legacySection().serialize(MinecraftSerializer.INSTANCE.serialize(in));
        } catch (NullPointerException | ConcurrentModificationException ex) {
            ex.printStackTrace();
            return in;
        }
    }

    public static Component convertMarkdownToMCFormattingComponent(String in) {
        try {
            return MinecraftSerializer.INSTANCE.serialize(in);
        } catch (NullPointerException | ConcurrentModificationException ex) {
            ex.printStackTrace();
            return Component.text().build();
        }
    }

    public static String convertMCToMarkdown(String in) {
        if (!Configuration.instance().messages.convertCodes) {
            if (Configuration.instance().messages.formattingCodesToDiscord) return in;
            else return TextFormatting.getTextWithoutFormattingCodes(in);
        }
        in = escapeMarkdownCodeBlocks(in);
        try {
            return DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacySection().deserialize(in));
        } catch (NullPointerException | ConcurrentModificationException ex) {
            ex.printStackTrace();
            return in;
        }
    }

    public static Component makeURLsClickable(final Component in) {
        return in.replaceText(URL_PATTERN, url -> {
            return url.decorate(TextDecoration.UNDERLINED).color(TextColor.color(0x06, 0x45, 0xAD)).clickEvent(ClickEvent.openUrl(url.content()));
        });
    }
}
