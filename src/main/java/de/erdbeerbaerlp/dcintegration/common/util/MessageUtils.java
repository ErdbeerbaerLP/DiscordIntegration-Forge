package de.erdbeerbaerlp.dcintegration.common.util;

import com.google.gson.JsonArray;
import com.vdurmont.emoji.EmojiParser;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerSettings;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
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

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

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

    /**
     * Gets the Discord name for the player
     * @param p Player UUID
     * @return Discord name, or null of the player did not link his discord account
     */
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

    /**
     * Escapes markdown from String
     * @param in String with markdown
     * @return Input string without markdown
     */
    public static String escapeMarkdown(String in) {
        return in.replace("(?<!\\\\)[`*_|~]/g", "\\\\$0");
    }
    /**
     * Escapes markdown codeblocks from String
     * @param in String with markdown codeblocks
     * @return Input string without markdown codeblocks
     */
    public static String escapeMarkdownCodeBlocks(String in) {
        return in.replace("(?<!\\\\)`/g", "\\\\$0");
    }

    /**
     * Gets the full server uptime formatted as specified in the config at {@link Configuration.Localization.Commands#uptimeFormat}
     * @return Uptime String
     */
    public static String getFullUptime() {
        if (Variables.started == 0) {
            return "?????";
        }
        final Duration duration = Duration.between(Instant.ofEpochMilli(Variables.started), Instant.now());
        return DurationFormatUtils.formatDuration(duration.toMillis(), Configuration.instance().localization.commands.uptimeFormat);
    }

    /**
     * Converts minecraft formatting codes into discord markdown
     * @param in String with mc formatting codes
     * @return String with markdown
     */
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

    /**
     * Removes all Minecraft formatting codes
     * @param text Formatted String
     * @return Unformatted String
     */
    public static String removeFormatting(String text) {
        return text == null ? null : FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Makes URLs in {@link Component}s clickable by adding click event and formatting
     * @param in Component which might contain an URL
     * @return Component with all URLs clickable
     */
    public static Component makeURLsClickable(final Component in) {
        return in.replaceText(TextReplacementConfig.builder().match(URL_PATTERN).replacement(url -> url.decorate(TextDecoration.UNDERLINED).color(TextColor.color(0x06, 0x45, 0xAD)).clickEvent(ClickEvent.openUrl(url.content()))).build());
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a String/Regex which should be replaced
     * @param b Replacement String
     * @return Configured {@link TextReplacementConfig}
     */
    public static TextReplacementConfig replace(String a, String b) {
        return TextReplacementConfig.builder().match(a).replacement(b).build();
    }
    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a Literal String which should be replaced
     * @param b Replacement String
     * @return Configured {@link TextReplacementConfig}
     */
    public static TextReplacementConfig replaceLiteral(String a, String b) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).build();
    }
    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a String/Regex which should be replaced
     * @param b Replacement String
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */
    public static TextReplacementConfig replace(String a, String b, int times) {
        return TextReplacementConfig.builder().match(a).replacement(b).times(times).build();
    }
    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a Literal String which should be replaced
     * @param b Replacement String
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */
    public static TextReplacementConfig replaceLiteral(String a, String b, int times) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).times(times).build();
    }
    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a String/Regex which should be replaced
     * @param b Replacement Component
     * @return Configured {@link TextReplacementConfig}
     */
    public static TextReplacementConfig replace(String a, Component b) {
        return TextReplacementConfig.builder().match(a).replacement(b).build();
    }
    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a Literal String which should be replaced
     * @param b Replacement Component
     * @return Configured {@link TextReplacementConfig}
     */
    public static TextReplacementConfig replaceLiteral(String a, Component b) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).build();
    }
    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a String/Regex which should be replaced
     * @param b Replacement Component
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */
    public static TextReplacementConfig replace(String a, Component b, int times) {
        return TextReplacementConfig.builder().match(a).replacement(b).times(times).build();
    }
    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a Literal String which should be replaced
     * @param b Replacement Component
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */
    public static TextReplacementConfig replaceLiteral(String a, Component b, int times) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).times(times).build();
    }

    /**
     * Parses and formats Pings/Mentions<br>Every found mention will get bold and colored in {@linkplain TextColors#PING}
     * @param msg Message where Mentions should be formatted from
     * @param uuid {@link UUID} of the receiving player
     * @param name Name of the receiving player
     * @return {@link Map.Entry} containing an boolean, which is true, when there was an mention found, as key and the formatted {@link Component} as value
     */
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

    /**
     * This regex will match all user mentions (<@userid>)
     */
    private static final Pattern USER_PING_REGEX = Pattern.compile("(<@!?([0-9]{17,18})>)");
    /**
     * This regex will match all role mentions (<@&roleid>)
     */
    private static final Pattern ROLE_PING_REGEX = Pattern.compile("(<@&([0-9]{17,20})>)");
    /**
     * This regex will match all channel mentions (<#channelid>)
     */
    private static final Pattern CHANNEL_REGEX = Pattern.compile("(<#([0-9]{17,20})>)");
    /**
     * This regex will match ANY type of mention
     */
    private static final Pattern ANYPING_REGEX = Pattern.compile("(<..?([0-9]{17,20})>)");

    /**
     * Translates ID mentions (like <@userid> to human-readable mentions (like @SomeName123)
     * @param in Component that should be formatted
     * @param targetGuild Target {@link Guild}, where the nicknames should be taken from
     * @return Formatted {@link Component}
     */
    public static Component mentionsToNames(Component in, final Guild targetGuild){
        in = in.replaceText(TextReplacementConfig.builder().match(ANYPING_REGEX).replacement((result,builder)->{
            builder.content(mentionsToNames(builder.content(),targetGuild));
            return builder;
        }).build());
        return in;
    }
    /**
     * Translates ID mentions (like <@userid> to human-readable mentions (like @SomeName123)
     * @param in String that should be formatted
     * @param targetGuild Target {@link Guild}, where the nicknames should be taken from
     * @return Formatted String
     */
    public static String mentionsToNames(String in, final Guild targetGuild) {
        final JDA jda = Variables.discord_instance.getJDA();
        if(jda == null) return in;  //Skip this if JDA wasn't initialized
        final Matcher userMatcher = USER_PING_REGEX.matcher(in);
        final Matcher roleMatcher = ROLE_PING_REGEX.matcher(in);
        final Matcher channelMatcher = CHANNEL_REGEX.matcher(in);
        while (userMatcher.find()) {
            final String str = userMatcher.group(1);
            final String id = userMatcher.group(2);
            final Member member = targetGuild.getMemberById(id);
            if (member == null) {
                final User user = jda.getUserById(id);
                if (user == null) continue;
                in = in.replace(str, "@" + user.getName());
            } else {
                in = in.replace(str, "@" + member.getEffectiveName());
            }
        }
        while (roleMatcher.find()) {
            final String str = roleMatcher.group(1);
            final String id = roleMatcher.group(2);
            final Role role = targetGuild.getRoleById(id);
            if (role == null) continue;
            in = in.replace(str, "@" + role.getName());
        }
        while (channelMatcher.find()) {
            final String str = channelMatcher.group(1);
            final String id = channelMatcher.group(2);
            final TextChannel channel = targetGuild.getTextChannelById(id);
            if (channel == null) continue;
            in = in.replace(str, "#" + channel.getName());
        }
        return in;
    }

    /**
     * Translates emotes and emojis into their text-form
     * @param emotes Array list of emotes that should be replaced (can be empty to only replace emojis)
     * @param msg Message with emotes and/or emojis
     * @return Formatted message
     */
    public static String formatEmoteMessage(List<Emote> emotes, String msg){
        msg = EmojiParser.parseToAliases(msg);
        for (final Emote e : emotes) {
            msg = msg.replace(e.toString(), ":" + e.getName() + ":");
        }
        return msg;
    }

    /**
     * Gets the display name of the player's UUID
     * @param uuid {@link UUID} to get the name from
     * @return The player's name, or null if the player was not found
     */
    @Nullable
    public static String getNameFromUUID(UUID uuid) {
        final String name = discord_instance.srv.getNameFromUUID(uuid);
        return name.isEmpty() ? null : name;
    }
}
