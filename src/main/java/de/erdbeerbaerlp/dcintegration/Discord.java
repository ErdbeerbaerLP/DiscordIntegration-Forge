package de.erdbeerbaerlp.dcintegration;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import de.erdbeerbaerlp.dcintegration.commands.DiscordCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.managers.ChannelManager;
import net.dv8tion.jda.internal.managers.ChannelManagerImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

import static de.erdbeerbaerlp.dcintegration.Configuration.*;

@SuppressWarnings("ConstantConditions")
public class Discord implements EventListener {
    private final JDA jda;
    public boolean isKilled = false;
    final ArrayList<String> ignoringPlayers = new ArrayList<>();
    /**
     * This thread is used to update the channel description
     */
    Thread updateChannelDesc = new Thread() {
        private String cachedDescription = "";

        {
            this.setName("[DC INTEGRATION] Channel Description Updater");
            this.setDaemon(false);
            this.setPriority(MAX_PRIORITY);
        }

        private double getAverageTickCount() {
            final MinecraftServer minecraftServer = ServerLifecycleHooks.getCurrentServer();
            //noinspection IntegerDivisionInFloatingPointContext
            return LongStream.of(minecraftServer.tickTimeArray).sum() / minecraftServer.tickTimeArray.length * 1.0E-6D;
        }

        private double getAverageTPS() {
            return Math.min(1000.0 / getAverageTickCount(), 20);
        }

        public void run() {
            try {
                while (true) {
                    final String newDesc = Configuration.INSTANCE.description.get().replace("%tps%", "" + Math.round(getAverageTPS())).replace("%online%", "" + ServerLifecycleHooks.getCurrentServer().getOnlinePlayerNames().length)
                            .replace("%max%", "" + ServerLifecycleHooks.getCurrentServer().getMaxPlayers()).replace("%motd%", ServerLifecycleHooks.getCurrentServer().getMOTD())
                            .replace("%uptime%", DiscordIntegration.getUptime());
                    if (!newDesc.equals(cachedDescription)) {
                        (ADVANCED.CHANNEL_DESCRIPTION_ID.isEmpty() ? getChannelManager() : getChannelManager(ADVANCED.CHANNEL_DESCRIPTION_ID)).setTopic(newDesc).complete();
                        cachedDescription = newDesc;
                    }
                    sleep(500);
                }
            } catch (InterruptedException | RuntimeException ignored) {

            }
        }
    };
    private List<DiscordCommand> commands = new ArrayList<>();
    /*/*
     * This thread is used to detect auto shutdown status using ftb utilities
     *//*
    Thread ftbUtilitiesShutdownDetectThread = new Thread() {
        {
            setName("[DC INTEGRATION] FTB Utilities shutdown detector");
            setDaemon(true);
            setPriority(MAX_PRIORITY);
        }

        public void run() {
            while (!isKilled) {
                final long timeLeft = TimeUnit.MILLISECONDS.toSeconds(FTBUtilitiesUniverseData.shutdownTime - Instant.now().toEpochMilli());
                if (timeLeft == 120)
                    sendMessage(Configuration.FTB_UTILITIES.SHUTDOWN_MSG_2MINUTES, Configuration.FTB_UTILITIES.FTB_AVATAR_ICON, "FTB Utilities");
                else if (timeLeft == 10) {
                    sendMessage(ADVANCED.FTB_UTILITIES_CHANNEL_ID.isEmpty() ? getChannel() : getChannel(ADVANCED.FTB_UTILITIES_CHANNEL_ID), Configuration.FTB_UTILITIES.SHUTDOWN_MSG_10SECONDS, Configuration.FTB_UTILITIES.FTB_AVATAR_ICON, "FTB Utilities");
                    break;
                }

                try {
                    sleep(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException ignored) {
                }
            }
            interrupt();
        }
    };
    /**
     * This thread is used to detect AFK states using ftb utilities
     *//*
    Thread ftbUtilitiesAFKDetectThread = new Thread() {
        {
            setName("[DC INTEGRATION] FTB Utilities AFK detector");
            setDaemon(true);
            setPriority(MAX_PRIORITY);
        }

        public void run() {
            if (!Configuration.FTB_UTILITIES.DISCORD_AFK_MSG_ENABLED) return;
            final Map<EntityPlayerMP, Entry<Long, Boolean>> timers = new HashMap<>();
            final Universe universe = Universe.get();
            while (!isKilled) {
                for (EntityPlayerMP player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                    try {
                        final FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(Objects.requireNonNull(universe.getPlayer(player)));
                        if (timers.containsKey(player) && data.afkTime < timers.get(player).getKey() && timers.get(player).getValue())
                            sendMessage(ADVANCED.FTB_UTILITIES_CHANNEL_ID.isEmpty() ? getChannel() : getChannel(ADVANCED.FTB_UTILITIES_CHANNEL_ID), Configuration.FTB_UTILITIES.DISCORD_AFK_MSG_END.replace("%player%", DiscordIntegration.formatPlayerName(player)), Configuration.FTB_UTILITIES.FTB_AVATAR_ICON, "FTB Utilities");
                        timers.put(player, new SimpleEntry<>(data.afkTime, (timers.containsKey(player) ? timers.get(player).getValue() : false)));
                    } catch (NullPointerException ignored) {
                    }
                }
                final List<EntityPlayerMP> toRemove = new ArrayList<>();
                timers.keySet().forEach((p) -> {
                    if (!ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().contains(p)) {
                        toRemove.add(p);
                    } else {
                        final boolean afk = timers.get(p).getKey() >= Ticks.get(FTBUtilitiesConfig.afk.notification_timer).millis();
                        if (afk && !timers.get(p).getValue())
                            sendMessage(ADVANCED.FTB_UTILITIES_CHANNEL_ID.isEmpty() ? getChannel() : getChannel(ADVANCED.FTB_UTILITIES_CHANNEL_ID), Configuration.FTB_UTILITIES.DISCORD_AFK_MSG.replace("%player%", DiscordIntegration.formatPlayerName(p)), Configuration.FTB_UTILITIES.FTB_AVATAR_ICON,
                                    "FTB " + "Utilities");
                        timers.put(p, new SimpleEntry<>(timers.get(p).getKey(), afk));

                    }
                });
                for (EntityPlayerMP p : toRemove) {
                    timers.remove(p);
                }
                try {
                    sleep(900);
                } catch (InterruptedException ignored) {
                }
            }
        }
    };
*/
    private ArrayList<String> messages = new ArrayList<>();
    /**
     * Thread to send messages from vanilla commands
     */
    Thread messageSender = new Thread(() -> {
        try {
            while (true) {
                if (!messages.isEmpty()) {
                    StringBuilder s = new StringBuilder();
                    for (final String msg : messages)
                        s.append(msg + "\n");
                    messages.clear();
                    this.sendMessage(s.toString().trim());
                }
                Thread.sleep(500);
            }
        } catch (InterruptedException ignored) {
        }
    });

    private static final File IGNORED_PLAYERS = new File("./players_ignoring_discord");

    /**
     * Constructor for this class
     */
    Discord() throws LoginException, InterruptedException {
        final JDABuilder b = new JDABuilder(Configuration.INSTANCE.botToken.get());
        b.setAutoReconnect(true);

        switch (Configuration.INSTANCE.botPresenceType.get()) {
            case DISABLED:
                break;
            case LISTENING:
                b.setActivity(Activity.listening(Configuration.INSTANCE.botPresenceName.get()));
                break;
            case PLAYING:
                b.setActivity(Activity.playing(Configuration.INSTANCE.botPresenceName.get()));
                break;
            case WATCHING:
                b.setActivity(Activity.watching(Configuration.INSTANCE.botPresenceName.get()));
                break;
        }
        b.setEnableShutdownHook(false);
        this.jda = b.build().awaitReady();
        System.out.println("Bot Ready");
        this.messageSender.start();
        jda.addEventListener(this);
        if (!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)) {
            System.err.println("ERROR! Bot does not have all permissions to work!");
            throw new PermissionException("Bot requires message read, message write, embed links and manage messages");
        }
        if (Configuration.INSTANCE.botModifyDescription.get()) if (!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_CHANNEL)) {
            Configuration.INSTANCE.botModifyDescription.set(false);
            System.err.println("ERROR! Bot does not have permission to manage channel, disabling channel description");
        }
        if (Configuration.INSTANCE.enableWebhook.get()) if (!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_WEBHOOKS)) {
            Configuration.INSTANCE.enableWebhook.set(false);
            System.err.println("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
        }
        try {
            loadIgnoreList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return an instance of the webhook or null
     */
    @Nullable
    public Webhook getWebhook(TextChannel c) {
        if (!Configuration.INSTANCE.enableWebhook.get()) return null;
        if (!PermissionUtil.checkPermission(c, c.getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_WEBHOOKS)) {
            Configuration.setValueAndSave(DiscordIntegration.cfg, Configuration.INSTANCE.enableWebhook.getPath(), false);
            System.out.println("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
            return null;
        }
        for (Webhook web : c.retrieveWebhooks().complete()) {
            if (web.getName().equals("MC_DISCORD_INTEGRATION")) {
                return web;
            }
        }
        return c.createWebhook("MC_DISCORD_INTEGRATION").complete();
    }

    /**
     * Sends a message when *not* using a webhook and returns it as RequestFuture<Message> or null when using a webhook
     *
     * @param msg message
     * @return Sent message
     */
    public CompletableFuture<Message> sendMessageReturns(String msg) {
        if (Configuration.INSTANCE.enableWebhook.get()) return null;
        else return getChannel().sendMessage(msg).submit();
    }

    /**
     * Constructor for this class
     */
    Discord() throws LoginException, InterruptedException {
        final JDABuilder b = new JDABuilder(GENERAL.BOT_TOKEN);
        b.setAutoReconnect(true);

        switch (GENERAL.BOT_GAME_TYPE) {
            case DISABLED:
                break;
            case LISTENING:
                b.setActivity(Activity.listening(GENERAL.BOT_GAME_NAME));
                break;
            case PLAYING:
                b.setActivity(Activity.playing(GENERAL.BOT_GAME_NAME));
                break;
            case WATCHING:
                b.setActivity(Activity.watching(GENERAL.BOT_GAME_NAME));
                break;
        }
        b.setEnableShutdownHook(false);
        this.jda = b.build().awaitReady();
        System.out.println("Bot Ready");
        this.messageSender.start();
        jda.addEventListener(this);
        if (!PermissionUtil.checkPermission(getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)) {
            System.err.println("ERROR! Bot does not have all permissions to work!");
            throw new PermissionException("Bot requires message read, message write, embed links and manage messages");
        }
        if (Configuration.GENERAL.MODIFY_CHANNEL_DESCRIPTRION)
            if (!PermissionUtil.checkPermission(getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_CHANNEL)) {
                Configuration.GENERAL.MODIFY_CHANNEL_DESCRIPTRION = false;
                System.err.println("ERROR! Bot does not have permission to manage channel, disabling channel description");
            }
        if (Configuration.WEBHOOK.BOT_WEBHOOK)
            if (!PermissionUtil.checkPermission(getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_WEBHOOKS)) {
                Configuration.WEBHOOK.BOT_WEBHOOK = false;
                System.err.println("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
            }
        try {
            loadIgnoreList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ChannelManager getChannelManager(String id) {
        return new ChannelManagerImpl(getChannel(id));
    }

    /**
     * Sends a message as player
     *
     * @param player Player
     * @param msg    Message
     */
    public void sendMessage(ServerPlayerEntity player, String msg) {
        sendMessage(DiscordIntegration.formatPlayerName(player), player.getUniqueID().toString(), msg, ADVANCED.CHAT_OUTPUT_ID.isEmpty() ? getChannel() : getChannel(ADVANCED.CHAT_OUTPUT_ID));
    }

    /**
     * Sends a message as server
     *
     * @param msg Message
     */
    public void sendMessage(String msg) {
        sendMessage(Configuration.INSTANCE.serverName.get(), "0000000", msg, ADVANCED.SERVER_CHANNEL_ID.isEmpty() ? getChannel() : getChannel(ADVANCED.SERVER_CHANNEL_ID));
    }

    /**
     * Sends a message to discord with custom avatar url (when using a webhook)
     *
     * @param msg       Message
     * @param avatarURL URL of the avatar image
     * @param name      Name of the fake player
     */
    public void sendMessage(String msg, String avatarURL, String name) {
        try {
            if (isKilled) return;
            if (Configuration.INSTANCE.enableWebhook.get()) {
                final WebhookMessageBuilder b = new WebhookMessageBuilder();
                b.setContent(msg);
                b.setUsername(name);
                b.setAvatarUrl(avatarURL);
                final WebhookClient cli = WebhookClient.withUrl(getWebhook(getChannel()).getUrl());
                cli.send(b.build());
                cli.close();
            } else {
                getChannel().sendMessage(Configuration.MESSAGES.PLAYER_CHAT_MSG.replace("%player%", name).replace("%msg%", msg)).queue();
            }
            else getChannel().sendMessage(Configuration.INSTANCE.msgChatMessage.get().replace("%player%", name).replace("%msg%", msg)).queue();
        } catch (Exception ignored) {
        }
    }

    /**
     * Sends a message to discord with custom avatar url (when using a webhook)
     *
     * @param msg       Message
     * @param avatarURL URL of the avatar image
     * @param name      Name of the fake player
     * @param ch        Channel to send message into
     */
    public void sendMessage(TextChannel ch, String msg, String avatarURL, String name) {
        try {
            if (isKilled) return;
            if (WEBHOOK.BOT_WEBHOOK) {
                final WebhookMessageBuilder b = new WebhookMessageBuilder();
                b.setContent(msg);
                b.setUsername(name);
                b.setAvatarUrl(avatarURL);
                final WebhookClient cli = WebhookClient.withUrl(getWebhook(ch).getUrl());
                cli.send(b.build());
                cli.close();
            } else {
                ch.sendMessage(Configuration.MESSAGES.PLAYER_CHAT_MSG.replace("%player%", name).replace("%msg%", msg)).queue();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Kills the discord bot
     */
    void kill() {
        stopThreads();
        this.isKilled = true;
        jda.shutdown();
    }
    /**
     * Sends a message to discord
     *
     * @param playerName the name of the player
     * @param UUID       the player uuid
     * @param msg        the message to send
     */
    public void sendMessage(String playerName, String UUID, String msg, TextChannel channel) {
        if (isKilled) return;
        if (!Configuration.INSTANCE.discordColorCodes.get()) msg = DiscordIntegration.stripControlCodes(msg);
        try {
            if (Configuration.INSTANCE.enableWebhook.get()) {
                if (playerName.equals(Configuration.INSTANCE.serverName.get()) && UUID.equals("0000000")) {
                    final WebhookMessageBuilder b = new WebhookMessageBuilder();
                    b.setContent(msg);
                    b.setUsername(Configuration.INSTANCE.serverName.get());
                    b.setAvatarUrl(Configuration.INSTANCE.serverAvatar.get());
                    final WebhookClient cli = WebhookClient.withUrl(getWebhook(channel).getUrl());
                    cli.send(b.build());
                    cli.close();
                } else {
                    final WebhookMessageBuilder b = new WebhookMessageBuilder();
                    b.setContent(msg);
                    b.setUsername(playerName);
                    b.setAvatarUrl("https://minotar.net/avatar/" + UUID);
                    final WebhookClient cli = WebhookClient.withUrl(getWebhook(channel).getUrl());
                    cli.send(b.build());
                    cli.close();
                }
            }
            else if (playerName.equals(Configuration.INSTANCE.serverName.get()) && UUID.equals("0000000")) {
                channel.sendMessage(msg).queue();
            }
            else {
                channel.sendMessage(Configuration.INSTANCE.msgChatMessage.get().replace("%player%", playerName).replace("%msg%", msg)).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Kills the discord bot
     */
    void kill() {
        stopThreads();
        this.isKilled = true;
        jda.shutdown();
    }
    
    final ArrayList<String> ignoringPlayers = new ArrayList<>();

    /**
     * Event handler to handle messages
     */
    @Override
    public void onEvent(GenericEvent event) {
        if (isKilled) return;
        if (event instanceof MessageReceivedEvent) {
            final MessageReceivedEvent ev = (MessageReceivedEvent) event;
            if (!ev.isWebhookMessage() && !ev.getAuthor().getId().equals(jda.getSelfUser().getId())) {
                if (ev.getMessage().getContentRaw().startsWith(Configuration.INSTANCE.prefix.get())) {
                    final String[] command = ev.getMessage().getContentRaw().replaceFirst(Configuration.INSTANCE.prefix.get(), "").split(" ");
                    String argumentsRaw = "";
                    for (int i = 1; i < command.length; i++) {
                        argumentsRaw = argumentsRaw + command[i] + " ";
                    }
                    argumentsRaw = argumentsRaw.trim();
                    boolean hasPermission = true;
                    boolean executed = false;
                    System.out.println(command[0]);
                    for (final DiscordCommand cmd : commands) {
                        if (!cmd.worksInChannel(ev.getTextChannel())) continue;
                        if (cmd.getName().equals(command[0])) {
                            if (cmd.canUserExecuteCommand(ev.getAuthor())) {
                                cmd.execute(argumentsRaw.split(" "), ev);
                                executed = true;
                            } else hasPermission = false;
                        }

                        for (final String alias : cmd.getAliases()) {
                            if (alias.equals(command[0])) {
                                if (cmd.canUserExecuteCommand(ev.getAuthor())) {
                                    cmd.execute(argumentsRaw.split(" "), ev);
                                    executed = true;
                                } else hasPermission = false;
                            }
                        }
                    }
                    if (!hasPermission) {
                        sendMessage(Configuration.INSTANCE.msgNoPermission.get(), ev.getTextChannel());
                        return;
                    }
                    if (!executed && (Configuration.INSTANCE.enableUnknownCommandEverywhere.get() || ev.getTextChannel().getId().equals(getChannel().getId())) && Configuration.INSTANCE.enableUnknownCommandMsg.get()) {
                        if (Configuration.INSTANCE.cmdHelpEnabled.get())
                            sendMessage(Configuration.INSTANCE.msgUnknownCommand.get().replace("%prefix%", Configuration.INSTANCE.prefix.get()), ev.getTextChannel());
                    }


                } else if (ev.getChannel().getId().equals(ADVANCED.CHAT_INPUT_ID.isEmpty() ? getChannel().getId() : ADVANCED.CHAT_INPUT_ID)) {
                    final List<MessageEmbed> embeds = ev.getMessage().getEmbeds();
                    String msg = ev.getMessage().getContentRaw();

                    for (final Member u : ev.getMessage().getMentionedMembers()) {
                        msg = msg.replace(Pattern.quote("<@" + u.getId() + ">"), "@" + u.getEffectiveName());
                    }
                    for (final Role r : ev.getMessage().getMentionedRoles()) {
                        msg = msg.replace(Pattern.quote("<@" + r.getId() + ">"), "@" + r.getName());
                    }
                    StringBuilder message = new StringBuilder(msg);
                    for (Message.Attachment a : ev.getMessage().getAttachments()) {
                        //noinspection StringConcatenationInsideStringBufferAppend
                        message.append("\nAttachment: " + a.getProxyUrl());
                    }
                    for (MessageEmbed e : embeds) {
                        if (e.isEmpty()) continue;
                        message.append("\n\n-----[Embed]-----\n");
                        if (e.getAuthor() != null && !e.getAuthor().getName().trim().isEmpty())
                            //noinspection StringConcatenationInsideStringBufferAppend
                            message.append(TextFormatting.BOLD + "" + TextFormatting.ITALIC + e.getAuthor().getName() + "\n");
                        if (e.getTitle() != null && !e.getTitle().trim().isEmpty())
                            //noinspection StringConcatenationInsideStringBufferAppend
                            message.append(TextFormatting.BOLD + e.getTitle() + "\n");
                        if (e.getDescription() != null && !e.getDescription().trim().isEmpty())
                            message.append("Message:\n").append(e.getDescription()).append("\n");
                        if (e.getImage() != null && !e.getImage().getProxyUrl().isEmpty())
                            message.append("Image: ").append(e.getImage().getProxyUrl()).append("\n");
                        message.append("\n-----------------");
                    }
                    sendMcMsg(ForgeHooks.newChatWithLinks(Configuration.INSTANCE.ingameDiscordMsg.get().replace("%user%", (ev.getMember() != null ? ev.getMember().getEffectiveName() : ev.getAuthor().getName()))
                                                                                                 .replace("%id%", ev.getAuthor().getId()).replace("%msg%", (Configuration.INSTANCE.preventMcColorCodes.get() ? DiscordIntegration
                                    .stripControlCodes(message.toString()) : message.toString())))
                            .setStyle(new Style().setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new StringTextComponent("Sent by discord user \"" + ev.getAuthor().getAsTag() + "\"")))));
                }
            }
        }
    }

    public boolean togglePlayerIgnore(PlayerEntity sender) {
        if (ignoringPlayers.contains(sender.getName().getUnformattedComponentText())) {
            ignoringPlayers.remove(sender.getName().getUnformattedComponentText());
            try {
                saveIgnoreList();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            ignoringPlayers.add(sender.getName().getUnformattedComponentText());
            return false;
        }
    }

    private void saveIgnoreList() throws IOException {
        if (!IGNORED_PLAYERS.exists() && !ignoringPlayers.isEmpty()) IGNORED_PLAYERS.createNewFile();
        if (!IGNORED_PLAYERS.exists() && ignoringPlayers.isEmpty()) {
            IGNORED_PLAYERS.delete();
            return;
        }
        FileWriter w = new FileWriter(IGNORED_PLAYERS);
        w.write("");
        for (String a : ignoringPlayers) {
            w.append(a).append("\n");
        }
        w.close();
    }


    public void loadIgnoreList() throws IOException {
        if (IGNORED_PLAYERS.exists()) {
            BufferedReader r = new BufferedReader(new FileReader(IGNORED_PLAYERS));
            r.lines().iterator().forEachRemaining(ignoringPlayers::add);
            r.close();
        }
    }

    private void sendMcMsg(final ITextComponent msg) {
        final List<ServerPlayerEntity> l = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        for (final ServerPlayerEntity p : l) {
            if (!ignoringPlayers.contains(p.getName().getUnformattedComponentText())) p.sendMessage(msg);
        }
    }

    public ChannelManager getChannelManager() {
        return getChannelManager(getChannel().getId());
    }

    /**
     * Registers an {@link DiscordCommand}
     *
     * @param cmd command
     * @return if the registration was successful
     */
    public boolean registerCommand(DiscordCommand cmd) {
        final ArrayList<DiscordCommand> toRemove = new ArrayList<>();
        for (DiscordCommand c : commands) {
            if (!cmd.isConfigCommand() && cmd.equals(c)) return false;
            else if (cmd.isConfigCommand() && cmd.equals(c)) toRemove.add(c);
        }
        for (DiscordCommand cm : toRemove)
            commands.remove(cm);
        return commands.add(cmd);
    }

    private void reRegisterAllCommands(final List<DiscordCommand> cmds) {
        System.out.println("Reloading " + cmds.size() + " commands");
        this.commands = cmds;

        for (DiscordCommand cmd : commands) {
            cmd.discord = DiscordIntegration.discord_instance;
        }

        System.out.println("Registered " + this.commands.size() + " commands");
    }

    /**
     * Restarts the discord bot (used by reload command)
     */
    public boolean restart() {
        try {
            kill();
            DiscordIntegration.discord_instance = new Discord();
            DiscordIntegration.discord_instance.reRegisterAllCommands(this.commands);
            DiscordIntegration.registerConfigCommands();
            DiscordIntegration.discord_instance.startThreads();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Starts all threads
     */
    public void startThreads() {
        System.out.println("Starting threads");
        if (Configuration.INSTANCE.botModifyDescription.get()) updateChannelDesc.start();
       /* if (!messageSender.isAlive()) messageSender.start();
        if (Loader.isModLoaded("ftbutilities")) {
            if (FTBUtilitiesConfig.auto_shutdown.enabled) ftbUtilitiesShutdownDetectThread.start();
            if (FTBUtilitiesConfig.afk.enabled) ftbUtilitiesAFKDetectThread.start();
        }*/
    }




    /**
     * Used to stop all discord integration threads in background
     */
    void stopThreads() {
//		if(ftbUtilitiesAFKDetectThread.isAlive()) ftbUtilitiesAFKDetectThread.interrupt();
        if (updateChannelDesc.isAlive()) updateChannelDesc.interrupt();
//		if(ftbUtilitiesShutdownDetectThread.isAlive()) ftbUtilitiesShutdownDetectThread.interrupt();
        if (messageSender.isAlive()) messageSender.interrupt();
    }

    /**
     * @return A list of all registered commands
     */
    public List<DiscordCommand> getCommandList() {
        return this.commands;
    }

    /**
     * @return The admin role of the server
     */
    public Role getAdminRole() {
        return (Configuration.INSTANCE.adminRoleId.get().equals("0") || Configuration.INSTANCE.adminRoleId.get().trim().isEmpty()) ? null : jda.getRoleById(Configuration.INSTANCE.adminRoleId.get());
    }


    /**
     * @return the specified text channel
     */
    public TextChannel getChannel() {
        return getChannel(Configuration.INSTANCE.botChannel.get());
    }

    /**
     * @return the specified text channel
     */
    public TextChannel getChannel(String id) {
        return jda.getTextChannelById(id);
    }

    /**
     * Adds messages to send in the next half second
     * Used by config commands
     *
     * @param msg message
     */
    public void sendMessageFuture(String msg) {
        this.messages.add(msg);
    }

    public void sendMessage(String msg, TextChannel textChannel) {
        this.sendMessage(Configuration.INSTANCE.serverName.get(), "0000000", msg, textChannel);
    }


    public enum GameTypes {
        WATCHING,
        PLAYING,
        LISTENING,
        //CUSTOM,
        DISABLED
    }
}

