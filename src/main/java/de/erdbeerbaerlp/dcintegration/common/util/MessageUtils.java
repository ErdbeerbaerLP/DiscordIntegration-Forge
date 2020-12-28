package de.erdbeerbaerlp.dcintegration.common.util;

import com.google.gson.JsonArray;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerSettings;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.PatternReplacementResult;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class MessageUtils {


    static final Pattern URL_PATTERN = Pattern.compile(
            //              schema                          ipv4            OR        namespace                 port     path         ends
            //        |-----------------|        |-------------------------|  |-------------------------|    |---------| |--|   |---------------|
            "((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"\u00A7 \n]|$))",
            Pattern.CASE_INSENSITIVE);

    public static String[] makeStringArray(final JsonArray channelID) {
        final String[] out = new String[channelID.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = channelID.get(i).getAsString();
        }
        return out;
    }

    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)\u00a7[0-9A-FK-OR]");

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

    public static String getFullUptime() {
        if (Variables.started == 0) {
            return "?????";
        }
        final Duration duration = Duration.between(Instant.ofEpochMilli(Variables.started), Instant.now());
        return DurationFormatUtils.formatDuration(duration.toMillis(), Configuration.instance().localization.commands.uptimeFormat);
    }

    public static String convertMCToMarkdown(String in) {
        if (!Configuration.instance().messages.convertCodes) {
            if (Configuration.instance().messages.formattingCodesToDiscord) return in;
            else return removeFormatting(in);
        }
        in = escapeMarkdownCodeBlocks(in);
        try {
            return DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacySection().deserialize(in));
        } catch (NullPointerException | ConcurrentModificationException ex) {
            ex.printStackTrace();
            return in;
        }
    }

    public static String removeFormatting(String text) {
        return text == null ? null : FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
    }

    public static Component makeURLsClickable(final Component in) {
        return in.replaceText(TextReplacementConfig.builder().match(URL_PATTERN).replacement(url -> url.decorate(TextDecoration.UNDERLINED).color(TextColor.color(0x06, 0x45, 0xAD)).clickEvent(ClickEvent.openUrl(url.content()))).build());
    }

    public static TextReplacementConfig replace(String a, String b) {
        return TextReplacementConfig.builder().match(a).replacement(b).build();
    }

    public static TextReplacementConfig replaceLiteral(String a, String b) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).build();
    }

    public static TextReplacementConfig replace(String a, String b, int times) {
        return TextReplacementConfig.builder().match(a).replacement(b).times(times).build();
    }

    public static TextReplacementConfig replaceLiteral(String a, String b, int times) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).times(times).build();
    }

    public static TextReplacementConfig replace(String a, Component b) {
        return TextReplacementConfig.builder().match(a).replacement(b).build();
    }

    public static TextReplacementConfig replaceLiteral(String a, Component b) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).build();
    }

    public static TextReplacementConfig replace(String a, Component b, int times) {
        return TextReplacementConfig.builder().match(a).replacement(b).times(times).build();
    }

    public static TextReplacementConfig replaceLiteral(String a, Component b, int times) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).times(times).build();
    }

    public static Map.Entry<Boolean, Component> parsePing(Component msg, UUID uuid, String name) {
        AtomicBoolean hasPing = new AtomicBoolean(false);
        msg = msg.replaceText(TextReplacementConfig.builder().matchLiteral("@" + name).replacement(Component.text("@" + name).style(Style.style(TextColors.PING).decorate(TextDecoration.BOLD))).condition((a, b) -> {
            hasPing.set(true);
            return PatternReplacementResult.REPLACE;
        }).build());
        if (!hasPing.get() && PlayerLinkController.isPlayerLinked(uuid)) {
            String dcname = Variables.discord_instance.getChannel().getGuild().getMember(Variables.discord_instance.getJDA().getUserById(PlayerLinkController.getDiscordFromPlayer(uuid))).getEffectiveName();
            msg = msg.replaceText(TextReplacementConfig.builder().matchLiteral("@" + dcname).replacement(Component.text("@" + dcname).style(Style.style(TextColors.PING).decorate(TextDecoration.BOLD))).condition((a, b) -> {
                hasPing.set(true);
                return PatternReplacementResult.REPLACE;
            }).build());
        }
        return new DefaultMapEntry<>(hasPing.get(), msg);
    }

}
