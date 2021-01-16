package de.erdbeerbaerlp.dcintegration.common.util;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.PatternReplacementResult;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ComponentUtils {
    @Nonnull
    public static Style addUserHoverClick(@Nonnull final Style styleIn, @Nonnull Member m){
        return addUserHoverClick(styleIn, m.getId(),m.getEffectiveName(),m.getUser().getAsTag());
    }
    @Nonnull
    public static Style addUserHoverClick(@Nonnull final Style styleIn, @Nonnull User u, @Nullable Member m){
        return addUserHoverClick(styleIn, u.getId(),m==null?u.getName():m.getEffectiveName(),u.getAsTag());

    }
    @Nonnull
    public static Style addUserHoverClick(@Nonnull final Style styleIn, @Nonnull String userID, @Nonnull String displayName, @Nonnull String tag){
        return styleIn.clickEvent(ClickEvent.suggestCommand("<@" + userID + ">")).hoverEvent(HoverEvent.showText(Component.text(Configuration.instance().localization.discordUserHover.replace("%user#tag%", tag).replace("%user%",displayName))));
    }

    /**
     * Makes URLs in {@link Component}s clickable by adding click event and formatting
     * @param in Component which might contain an URL
     * @return Component with all URLs clickable
     */
    @Nonnull
    public static Component makeURLsClickable(@Nonnull final Component in) {
        return in.replaceText(TextReplacementConfig.builder().match(MessageUtils.URL_PATTERN).replacement(url -> url.decorate(TextDecoration.UNDERLINED).color(TextColor.color(0x06, 0x45, 0xAD)).clickEvent(ClickEvent.openUrl(url.content()))).build());
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a String/Regex which should be replaced
     * @param b Replacement String
     * @return Configured {@link TextReplacementConfig}
     */
    @Nonnull
    public static TextReplacementConfig replace(@Nonnull String a, @Nonnull String b) {
        return TextReplacementConfig.builder().match(a).replacement(b).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a Literal String which should be replaced
     * @param b Replacement String
     * @return Configured {@link TextReplacementConfig}
     */
    @Nonnull
    public static TextReplacementConfig replaceLiteral(@Nonnull String a, @Nonnull String b) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a String/Regex which should be replaced
     * @param b Replacement String
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */
    @Nonnull
    public static TextReplacementConfig replace(@Nonnull String a, @Nonnull String b, int times) {
        return TextReplacementConfig.builder().match(a).replacement(b).times(times).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a Literal String which should be replaced
     * @param b Replacement String
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */
    @Nonnull
    public static TextReplacementConfig replaceLiteral(@Nonnull String a, @Nonnull String b, int times) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).times(times).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a String/Regex which should be replaced
     * @param b Replacement Component
     * @return Configured {@link TextReplacementConfig}
     */
    @Nonnull
    public static TextReplacementConfig replace(@Nonnull String a, @Nonnull Component b) {
        return TextReplacementConfig.builder().match(a).replacement(b).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a Literal String which should be replaced
     * @param b Replacement Component
     * @return Configured {@link TextReplacementConfig}
     */
    @Nonnull
    public static TextReplacementConfig replaceLiteral(@Nonnull String a, @Nonnull Component b) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a String/Regex which should be replaced
     * @param b Replacement Component
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */
    @Nonnull
    public static TextReplacementConfig replace(@Nonnull String a, @Nonnull Component b, int times) {
        return TextReplacementConfig.builder().match(a).replacement(b).times(times).build();
    }

    /**
     * Provides an {@link TextReplacementConfig} for {@link Component#replaceText(TextReplacementConfig)}
     * @param a Literal String which should be replaced
     * @param b Replacement Component
     * @param times Only replaces this amount of matches
     * @return Configured {@link TextReplacementConfig}
     */
    @Nonnull
    public static TextReplacementConfig replaceLiteral(@Nonnull String a, @Nonnull Component b, int times) {
        return TextReplacementConfig.builder().matchLiteral(a).replacement(b).times(times).build();
    }

    /**
     * Parses and formats Pings/Mentions<br>Every found mention will get bold and colored in {@linkplain TextColors#PING}
     * @param msg Message where Mentions should be formatted from
     * @param uuid {@link UUID} of the receiving player
     * @param name Name of the receiving player
     * @return {@link Map.Entry} containing an boolean, which is true, when there was an mention found, as key and the formatted {@link Component} as value
     */
    @Nonnull
    public static Map.Entry<Boolean, Component> parsePing(@Nonnull Component msg, @Nonnull UUID uuid, @Nonnull String name) {
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
