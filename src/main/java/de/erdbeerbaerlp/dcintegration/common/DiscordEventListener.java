package de.erdbeerbaerlp.dcintegration.common;

import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.inChat.DiscordCommand;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs.DMCommand;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions;
import dev.vankka.mcdiscordreserializer.rules.DiscordMarkdownRules;
import dev.vankka.simpleast.core.node.Node;
import dev.vankka.simpleast.core.node.TextNode;
import dev.vankka.simpleast.core.parser.ParseSpec;
import dev.vankka.simpleast.core.parser.Parser;
import dev.vankka.simpleast.core.parser.Rule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DiscordEventListener implements EventListener {

    static final MinecraftSerializerOptions options;

    static {
        List<Rule<Object, Node<Object>, Object>> rules = new ArrayList<>(DiscordMarkdownRules.createAllRulesForDiscord(false));
        rules.add(new Rule<Object, Node<Object>, Object>(Pattern.compile("(.*)")) {
            @Override
            public ParseSpec<Object, Node<Object>, Object> parse(Matcher matcher, Parser<Object, Node<Object>, Object> parser, Object state) {
                return ParseSpec.createTerminal(new TextNode<>(matcher.group()), state);
            }
        });
        options = MinecraftSerializerOptions.defaults().withRules(rules);
    }

    /**
     * Event handler to handle messages
     */
    @Override
    public void onEvent(GenericEvent event) {
        final Discord dc = Variables.discord_instance;
        final JDA jda = dc.getJDA();
        if (jda == null) return;
        if (event instanceof MessageReceivedEvent) {
            final MessageReceivedEvent ev = (MessageReceivedEvent) event;
            if (ev.getChannelType().equals(ChannelType.TEXT)) {
                if (!ev.isWebhookMessage() && !ev.getAuthor().getId().equals(jda.getSelfUser().getId())) {
                    for (DiscordEventHandler o : Variables.eventHandlers) {
                        if (o.onDiscordMessagePre(ev)) return;
                    }
                    if (ev.getMessage().getContentRaw().startsWith(Configuration.instance().commands.prefix + (Configuration.instance().commands.spaceAfterPrefix ? " " : ""))) {
                        final String[] command = ev.getMessage().getContentRaw().replaceFirst(Configuration.instance().commands.prefix, "").split(" ");
                        String argumentsRaw = "";
                        for (int i = 1; i < command.length; i++) {
                            argumentsRaw = argumentsRaw + command[i] + " ";
                        }
                        argumentsRaw = argumentsRaw.trim();
                        boolean hasPermission = true;
                        boolean executed = false;
                        for (final DiscordCommand cmd : CommandRegistry.getCommandList()) {
                            if (!cmd.worksInChannel(ev.getTextChannel())) {
                                continue;
                            }
                            if (cmd.getName().equals(command[0])) {
                                if (cmd.canUserExecuteCommand(ev.getAuthor())) {
                                    for (DiscordEventHandler o : Variables.eventHandlers) {
                                        if (o.onDiscordCommand(ev, cmd)) return;
                                    }
                                    cmd.execute(argumentsRaw.split(" "), ev);
                                    executed = true;
                                } else {
                                    hasPermission = false;
                                }
                            }
                            for (final String alias : cmd.getAliases()) {
                                if (alias.equals(command[0])) {
                                    if (cmd.canUserExecuteCommand(ev.getAuthor())) {
                                        for (DiscordEventHandler o : Variables.eventHandlers) {
                                            if (o.onDiscordCommand(ev, cmd)) return;
                                        }
                                        cmd.execute(argumentsRaw.split(" "), ev);
                                        executed = true;
                                    } else {
                                        hasPermission = false;
                                    }
                                }
                            }
                        }
                        if (!executed)
                            for (DiscordEventHandler o : Variables.eventHandlers) {
                                if (o.onDiscordCommand(ev, null)) return;
                            }
                        if (!hasPermission) {
                            dc.sendMessage(Configuration.instance().localization.noPermission, ev.getTextChannel());
                            return;
                        }
                        if (!executed && (Configuration.instance().commands.showUnknownCommandEverywhere || ev.getTextChannel().getId().equals(dc.getChannel().getId())) && Configuration.instance().commands.showUnknownCommandMessage) {
                            if (Configuration.instance().commands.helpCmdEnabled)
                                dc.sendMessage(Configuration.instance().localization.unknownCommand.replace("%prefix%", Configuration.instance().commands.prefix), ev.getTextChannel());
                        }


                    } else if (ev.getChannel().getId().equals(Configuration.instance().advanced.chatInputChannelID.equals("default") ? dc.getChannel().getId() : Configuration.instance().advanced.chatInputChannelID)) {
                        final List<MessageEmbed> embeds = ev.getMessage().getEmbeds();
                        String msg = ev.getMessage().getContentRaw();

                        for (final Member u : ev.getMessage().getMentionedMembers()) {
                            msg = msg.replace(Pattern.quote(u.getAsMention()), "@" + u.getEffectiveName());
                        }
                        //Replace user mentions in case it wasn't caught by member mentions
                        for (final User u : ev.getMessage().getMentionedUsers()) {
                            msg = msg.replace(Pattern.quote(u.getAsMention()), "@" + u.getName());
                        }
                        for (final Role r : ev.getMessage().getMentionedRoles()) {
                            msg = msg.replace(Pattern.quote("<@" + r.getId() + ">"), "@" + r.getName());
                        }
                        msg = dc.srv.formatEmoteMessage(ev.getMessage().getEmotes(), msg);
                        Component attachmentComponent = Component.text("\n");
                        if (!ev.getMessage().getAttachments().isEmpty())
                            attachmentComponent.append(Component.text("Attachments:").decorate(TextDecoration.UNDERLINED));
                        for (Message.Attachment a : ev.getMessage().getAttachments()) {
                            System.out.println(a);
                            attachmentComponent.append(Component.text(a.getFileName()).decorate(TextDecoration.UNDERLINED).color(TextColor.color(0x06, 0x45, 0xAD)).clickEvent(ClickEvent.openUrl(a.getUrl())));
                            attachmentComponent.append(Component.text("\n"));
                        }
                        for (MessageEmbed e : embeds) {
                            if (e.isEmpty()) continue;
                            attachmentComponent.append(Component.text("\n-----[Embed]-----\n"));
                            if (e.getAuthor() != null && !e.getAuthor().getName().trim().isEmpty()) {
                                attachmentComponent.append(Component.text(e.getAuthor().getName() + "\n").decorate(TextDecoration.BOLD).decorate(TextDecoration.ITALIC));
                            }
                            if (e.getTitle() != null && !e.getTitle().trim().isEmpty()) {
                                attachmentComponent.append(Component.text(e.getTitle() + "\n").decorate(TextDecoration.BOLD));
                            }
                            if (e.getDescription() != null && !e.getDescription().trim().isEmpty()) {
                                attachmentComponent.append(Component.text("Message:\n" + e.getDescription() + "\n"));
                            }
                            if (e.getImage() != null && e.getImage().getUrl() != null && !e.getImage().getUrl().isEmpty()) {
                                attachmentComponent.append(Component.text("Image: " + e.getImage().getUrl() + "\n"));
                            }
                            attachmentComponent.append(Component.text("\n-----------------"));
                        }

                        Component out = Component.text(Configuration.instance().localization.ingame_discordMessage);
                        final Component outMsg = MinecraftSerializer.INSTANCE.serialize(msg.replace("\n", "\\n"), options);
                        final int memberColor = (ev.getMember() != null ? ev.getMember().getColorRaw() : 0);
                        out = out.replaceText("%user%", Component.text((ev.getMember() != null ? ev.getMember().getEffectiveName() : ev.getAuthor().getName())).color(TextColor.color(memberColor)).hoverEvent(HoverEvent.showText(Component.text("Sent by Discord user @" + ev.getAuthor().getAsTag()))));
                        out = out.replaceText("%msg%", MessageUtils.makeURLsClickable(outMsg)).replaceText("%id%", Component.text(ev.getAuthor().getId()));
                        out.append(attachmentComponent);
                        dc.srv.sendMCMessage(out);
                    }
                }
                for (DiscordEventHandler o : Variables.eventHandlers) {
                    o.onDiscordMessagePost(ev);
                }
            } else if (ev.getChannelType().equals(ChannelType.PRIVATE)) {
                if (!ev.getAuthor().getId().equals(jda.getSelfUser().getId())) {
                    for (DiscordEventHandler o : Variables.eventHandlers) {
                        if (o.onDiscordPrivateMessage(ev)) return;
                    }
                    if (ev.getMessage().getContentRaw().startsWith(Configuration.instance().commands.prefix + (Configuration.instance().commands.spaceAfterPrefix ? " " : ""))) {
                        final String[] command = ev.getMessage().getContentRaw().replaceFirst(Configuration.instance().commands.prefix, "").split(" ");
                        String argumentsRaw = "";
                        for (int i = 1; i < command.length; i++) {
                            argumentsRaw += command[i] + " ";
                        }
                        argumentsRaw = argumentsRaw.trim();
                        boolean hasPermission = true;
                        boolean executed = false;
                        for (final DMCommand cmd : CommandRegistry.getDMCommandList()) {
                            if (cmd.getName().equals(command[0])) {
                                if (cmd.canUserExecuteCommand(ev.getAuthor())) {
                                    for (DiscordEventHandler o : Variables.eventHandlers) {
                                        if (o.onDiscordDMCommand(ev, cmd)) return;
                                    }
                                    cmd.execute(argumentsRaw.split(" "), ev);
                                    executed = true;
                                } else {
                                    hasPermission = false;
                                }
                            }
                            for (final String alias : cmd.getAliases()) {
                                if (alias.equals(command[0])) {
                                    if (cmd.canUserExecuteCommand(ev.getAuthor())) {
                                        for (DiscordEventHandler o : Variables.eventHandlers) {
                                            if (o.onDiscordDMCommand(ev, cmd)) return;
                                        }
                                        cmd.execute(argumentsRaw.split(" "), ev);
                                        executed = true;
                                    } else {
                                        hasPermission = false;
                                    }
                                }
                            }
                        }
                        if (!executed)
                            for (DiscordEventHandler o : Variables.eventHandlers) {
                                if (o.onDiscordCommand(ev, null)) return;
                            }
                        if (!hasPermission) {
                            ev.getChannel().sendMessage(Configuration.instance().localization.notLinked).queue();
                            return;
                        }
                        if (!executed)
                            ev.getChannel().sendMessage(Configuration.instance().localization.unknownCommand.replace("%prefix%", Configuration.instance().commands.prefix)).queue();
                    }
                }
            }
        }
    }
}
