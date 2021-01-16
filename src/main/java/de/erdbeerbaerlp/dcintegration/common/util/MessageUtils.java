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
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

public class MessageUtils {


    static final Pattern URL_PATTERN = Pattern.compile(
            //              schema                          ipv4            OR        namespace                 port     path         ends
            //        |-----------------|        |-------------------------|  |-------------------------|    |---------| |--|   |---------------|
            "((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"\u00A7 \n]|$))",
            Pattern.CASE_INSENSITIVE);

    @Nonnull
    public static String[] makeStringArray(@Nonnull final JsonArray channelID) {
        final String[] out = new String[channelID.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = channelID.get(i).getAsString();
        }
        return out;
    }

    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)\u00a7[0-9A-FK-OR]");

    /**
     * Gets the Discord name for the player
     *
     * @param p Player UUID
     * @return Discord name, or null of the player did not link his discord account OR disabled useDiscordNameInChannel
     */
    @Nullable
    public static String getDiscordName(@Nonnull final UUID p) {
        if (Variables.discord_instance == null) return null;
        if (Configuration.instance().linking.enableLinking && PlayerLinkController.isPlayerLinked(p)) {
            final PlayerSettings settings = PlayerLinkController.getSettings(null, p);
            if (settings.useDiscordNameInChannel) {
                return Variables.discord_instance.getChannel().getGuild().getMemberById(PlayerLinkController.getDiscordFromPlayer(p)).getEffectiveName();
            }
        }
        return null;
    }

    /**
     * Escapes markdown from String
     *
     * @param in String with markdown
     * @return Input string without markdown
     */
    @Nonnull
    public static String escapeMarkdown(@Nonnull String in) {
        return in.replace("(?<!\\\\)[`*_|~]/g", "\\\\$0");
    }

    /**
     * Escapes markdown codeblocks from String
     *
     * @param in String with markdown codeblocks
     * @return Input string without markdown codeblocks
     */
    @Nonnull
    public static String escapeMarkdownCodeBlocks(@Nonnull String in) {
        return in.replace("(?<!\\\\)`/g", "\\\\$0");
    }

    /**
     * Gets the full server uptime formatted as specified in the config at {@link Configuration.Localization.Commands#uptimeFormat}
     * @return Uptime String
     */
    @Nonnull
    public static String getFullUptime() {
        if (Variables.started == 0) {
            return "?????";
        }
        final Duration duration = Duration.between(Instant.ofEpochMilli(Variables.started), Instant.now());
        return DurationFormatUtils.formatDuration(duration.toMillis(), Configuration.instance().localization.commands.uptimeFormat);
    }

    /**
     * Converts minecraft formatting codes into discord markdown
     *
     * @param in String with mc formatting codes
     * @return String with markdown
     */
    @Nonnull
    public static String convertMCToMarkdown(@Nonnull String in) {
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
     *
     * @param text Formatted String
     * @return Unformatted String
     */
    @Nonnull
    public static String removeFormatting(@Nonnull String text) {
        return FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
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
     *
     * @param in          Component that should be formatted
     * @param targetGuild Target {@link Guild}, where the nicknames should be taken from
     * @return Formatted {@link Component}
     */
    @Nonnull
    public static Component mentionsToNames(@Nonnull Component in, @Nonnull final Guild targetGuild) {
        in = in.replaceText(TextReplacementConfig.builder().match(ANYPING_REGEX).replacement((result, builder) -> {
            builder.content(mentionsToNames(builder.content(), targetGuild));
            return builder;
        }).build());
        return in;
    }

    /**
     * Translates ID mentions (like <@userid> to human-readable mentions (like @SomeName123)
     *
     * @param in          String that should be formatted
     * @param targetGuild Target {@link Guild}, where the nicknames should be taken from
     * @return Formatted String
     */
    @Nonnull
    public static String mentionsToNames(@Nonnull String in, @Nonnull final Guild targetGuild) {
        final JDA jda = Variables.discord_instance.getJDA();
        if (jda == null) return in;  //Skip this if JDA wasn't initialized
        final Matcher userMatcher = USER_PING_REGEX.matcher(in);
        final Matcher roleMatcher = ROLE_PING_REGEX.matcher(in);
        final Matcher channelMatcher = CHANNEL_REGEX.matcher(in);
        while (userMatcher.find()) {
            final String str = userMatcher.group(1);
            final String id = userMatcher.group(2);
            String name;
            final User u = jda.getUserById(id);
            if (u != null) {
                final Member m = targetGuild.getMember(u);
                if (m != null)
                    name = m.getEffectiveName();
                else
                    name = u.getName();
                in = in.replace(str, "@" + name);
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
     *
     * @param emotes Array list of emotes that should be replaced (can be empty to only replace emojis)
     * @param msg    Message with emotes and/or emojis
     * @return Formatted message
     */
    @Nonnull
    public static String formatEmoteMessage(@Nonnull List<Emote> emotes, @Nonnull String msg) {
        msg = EmojiParser.parseToAliases(msg);
        for (final Emote e : emotes) {
            msg = msg.replace(e.toString(), ":" + e.getName() + ":");
        }
        return msg;
    }

    /**
     * Gets the display name of the player's UUID
     *
     * @param uuid {@link UUID} to get the name from
     * @return The player's name, or null if the player was not found
     */
    @Nullable
    public static String getNameFromUUID(@Nonnull UUID uuid) {
        final String name = discord_instance.srv.getNameFromUUID(uuid);
        return name == null || name.isEmpty() ? null : name;
    }
}
