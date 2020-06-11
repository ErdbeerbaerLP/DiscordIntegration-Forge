package de.erdbeerbaerlp.dcintegration.integrations;

import net.dv8tion.jda.api.entities.Emote;
import net.teamfruit.emojicord.emoji.EmojiText;
import net.teamfruit.emojicord.util.Base64Utils;

import java.util.EnumSet;

public class EmojicordFormatter {
    public static String formatChatToDiscord(final String text) {
        String out = text;
        final EmojiText emojiText = EmojiText.create(text, EnumSet.of(EmojiText.ParseFlag.PARSE));
        for (EmojiText.EmojiTextElement emoji : emojiText.emojis) {
            if (emoji.id == null)
                out = text.replace(emoji.raw, emoji.source);
            else
                out = text.replace(emoji.raw, "<" + emoji.source
                        + emoji.id.getId() + ">");
        }
        return out;
    }

    public static String formatDiscordToChat(final String in, final Emote emote) {
        String out = in;
        if (out.contains(emote.getAsMention())) {
            final String s = Base64Utils.encode(emote.getIdLong());
            out = out.replace(emote.getAsMention(), emote.getAsMention().replace(emote.getId(), s));
        }
        return out;
    }
}
