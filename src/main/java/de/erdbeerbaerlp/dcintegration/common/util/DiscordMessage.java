package de.erdbeerbaerlp.dcintegration.common.util;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import javax.annotation.Nonnull;

public final class DiscordMessage {
    private final boolean isNotRaw;
    private MessageEmbed embed;

    private String message = "";

    /**
     * @param isNotRaw set to true to enable markdown escaping and mc color conversion (default: false)
     */
    public DiscordMessage(final MessageEmbed embed, @Nonnull final String message, boolean isNotRaw) {
        this.embed = embed;
        this.message = message;
        this.isNotRaw = isNotRaw;
    }

    public DiscordMessage(final MessageEmbed embed, @Nonnull final String message) {
        this(embed, message, false);
    }

    public DiscordMessage(@Nonnull final String message) {
        this(null, message, false);
    }

    public DiscordMessage(@Nonnull final MessageEmbed embed) {
        this(embed, "", false);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(@Nonnull final String message) {
        this.message = message;
    }

    public MessageEmbed getEmbed() {
        return embed;
    }

    public void setEmbed(@Nonnull final MessageEmbed embed) {
        this.embed = embed;
    }
    @Nonnull
    public Message buildMessage() {
        final MessageBuilder out = new MessageBuilder();
        if (!message.isEmpty()) {
            if (isNotRaw) {
                if (Configuration.instance().messages.formattingCodesToDiscord)
                    out.setContent(MessageUtils.convertMCToMarkdown(message));
                else
                    out.setContent(MessageUtils.removeFormatting(MessageUtils.convertMCToMarkdown(message)));
            } else {
                out.setContent(message);
            }
        }
        if (embed != null)
            out.setEmbed(embed);
        return out.build();
    }
    @Nonnull
    public WebhookMessageBuilder buildWebhookMessage() {
        final WebhookMessageBuilder out = new WebhookMessageBuilder();
        if (!message.isEmpty()) {
            if (isNotRaw) {
                if (Configuration.instance().messages.formattingCodesToDiscord)
                    out.setContent(MessageUtils.convertMCToMarkdown(message));
                else
                    out.setContent(MessageUtils.removeFormatting(MessageUtils.convertMCToMarkdown(message)));
            } else {
                out.setContent(message);
            }
        }
        if (embed != null) {
            final WebhookEmbedBuilder eb = new WebhookEmbedBuilder();
            if (embed.getAuthor() != null)
                eb.setAuthor(new WebhookEmbed.EmbedAuthor(embed.getAuthor().getName(), embed.getAuthor().getIconUrl(), embed.getAuthor().getUrl()));
            eb.setColor(embed.getColorRaw());
            eb.setDescription(embed.getDescription());
            if (embed.getFooter() != null)
                eb.setFooter(new WebhookEmbed.EmbedFooter(embed.getFooter().getText(), embed.getFooter().getIconUrl()));
            if (embed.getImage() != null)
                eb.setImageUrl(embed.getImage().getUrl());
            if (embed.getThumbnail() != null)
                eb.setThumbnailUrl(embed.getThumbnail().getUrl());
            for (MessageEmbed.Field f : embed.getFields()) {
                eb.addField(new WebhookEmbed.EmbedField(f.isInline(), f.getName(), f.getValue()));
            }
            eb.setTimestamp(embed.getTimestamp());
            if (embed.getTitle() != null)
                eb.setTitle(new WebhookEmbed.EmbedTitle(embed.getTitle(), embed.getUrl()));
            out.addEmbeds(eb.build());
        }
        return out;
    }
}
