package de.erdbeerbaerlp.dcintegration.common;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerSettings;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.ServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.*;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;


public class Discord extends Thread {

    /**
     * Cache file for players which ignore discord chat
     */
    private static final File IGNORED_PLAYERS = new File(Variables.discordDataDir, ".PlayerIgnores");

    /**
     * ArrayList with players which ignore the discord chat
     */
    public final ArrayList<UUID> ignoringPlayers = new ArrayList<>();
    /**
     * Pending /discord link requests
     */
    public final HashMap<Integer, KeyValue<Instant, UUID>> pendingLinks = new HashMap<>();
    final ServerInterface srv;
    /**
     * Pending messages from command sender
     */
    private final HashMap<String, ArrayList<String>> messages = new HashMap<>();

    private final HashMap<String, Webhook> webhookHashMap = new HashMap<>();
    private final HashMap<String, WebhookClient> webhookClis = new HashMap<>();
    /**
     * Current JDA instance
     */
    private JDA jda = null;
    private Thread messageSender, statusUpdater;

    public Discord(ServerInterface srv) {
        this.srv = srv;
        setDaemon(true);
        setName("Discord Integration Launch Thread");
        start();
    }


    @Override
    public void run() {
        while (true) {
            final JDABuilder b = JDABuilder.createDefault(Configuration.instance().general.botToken);
            b.setAutoReconnect(true);
            b.setEnableShutdownHook(false);
            b.enableIntents(GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES);
            b.setMemberCachePolicy(MemberCachePolicy.ALL);
            b.setChunkingFilter(ChunkingFilter.ALL);
            try {
                jda = b.build();
                jda.awaitReady();
                break;
            } catch (LoginException e) {
                if (e.getMessage().equals("The provided token is invalid!")) {
                    System.err.println("Invalid token, please set correct token in the config file!");
                    return;
                }
                System.err.println("Login failed, retrying");
                try {
                    sleep(6000);
                } catch (InterruptedException ignored) {
                    return;
                }
            } catch (InterruptedException | IllegalStateException e) {
                return;
            }
        }
        System.out.println("Bot Ready");
        jda.addEventListener(new DiscordEventListener());

        if (!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)) {
            System.err.println("ERROR! Bot does not have all permissions to work!");
            throw new PermissionException("Bot requires message read, message write, embed links and manage messages");
        }
        if (Configuration.instance().webhook.enable)
            if (!PermissionUtil.checkPermission(getChannel(), getChannel().getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_WEBHOOKS)) {
                System.err.println("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
                Configuration.instance().webhook.enable = false;
                Configuration.instance().saveConfig();
            }
        try {
            loadIgnoreList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WebhookClient getWebhookCli(String channel) {
        return webhookClis.computeIfAbsent(channel, (id) -> WebhookClient.withUrl(getWebhook(getChannel(id)).getUrl()));
    }

    /**
     * Kills the discord bot
     */
    public void kill() {
        if (jda != null) {
            stopThreads();
            webhookClis.forEach((i, w) -> w.close());
            jda.shutdownNow();
            jda = null;
        }
    }

    /**
     * @return The admin role of the server
     */
    public Role getAdminRole() {
        return (Configuration.instance().commands.adminRoleID.equals("0") || Configuration.instance().commands.adminRoleID.trim().isEmpty()) ? null : jda.getRoleById(Configuration.instance().commands.adminRoleID);
    }

    /**
     * @return the specified text channel
     */
    public TextChannel getChannel() {
        return getChannel(Configuration.instance().general.botChannel);
    }

    /**
     * @return the specified text channel
     */
    public TextChannel getChannel(String id) {
        if (jda == null) return null;
        return jda.getTextChannelById(id);
    }


    /**
     * Loads the last known players who ignored discord messages from file
     *
     * @throws IOException
     */
    public void loadIgnoreList() throws IOException {
        if (IGNORED_PLAYERS.exists()) {
            BufferedReader r = new BufferedReader(new FileReader(IGNORED_PLAYERS));
            r.lines().iterator().forEachRemaining((s) -> {
                try {
                    ignoringPlayers.add(UUID.fromString(s));
                } catch (IllegalArgumentException e) {
                    System.err.println("Found invalid entry for ignoring player, skipping");
                }
            });
            r.close();
        }
    }

    /**
     * Starts all sub-threads
     */
    public void startThreads() {
        if (statusUpdater == null) statusUpdater = new Discord.StatusUpdateThread();
        if (messageSender == null) messageSender = new Discord.MessageQueueThread();
        if (!messageSender.isAlive()) messageSender.start();
        if (!statusUpdater.isAlive()) statusUpdater.start();
    }

    /**
     * Stops all sub-threads
     */
    public void stopThreads() {
        if (messageSender != null && messageSender.isAlive()) messageSender.interrupt();
        if (statusUpdater != null && statusUpdater.isAlive()) statusUpdater.interrupt();
        if (isAlive()) interrupt();
    }

    /**
     * @return Current JDA instance
     */
    public JDA getJDA() {
        return jda;
    }

    /**
     * Adds messages to send in the next half second
     * Used by config commands
     *
     * @param msg       message
     * @param channelID the channel ID the message should get sent to
     */
    public void sendMessageFuture(String msg, String channelID) {
        if (msg.isEmpty() || channelID.isEmpty()) return;
        final ArrayList<String> msgs;
        if (messages.containsKey(channelID))
            msgs = messages.get(channelID);
        else
            msgs = new ArrayList<>();
        msgs.add(msg);
        messages.put(channelID, msgs);
    }

    /**
     * Sends a message embed as player
     *
     * @param playerName Player Name
     * @param embed      Discord embed
     * @param channel    Target channel
     * @param uuid       Player UUID
     */
    public void sendMessage(final String playerName, String uuid, MessageEmbed embed, TextChannel channel) {
        sendMessage(playerName, uuid, new DiscordMessage(embed), channel);
    }

    /**
     * Sends a message as server
     *
     * @param msg Message
     */
    public void sendMessage(String msg) {
        sendMessage(Configuration.instance().webhook.serverName, "0000000", msg, Configuration.instance().advanced.serverChannelID.equals("default") ? getChannel() : getChannel(Configuration.instance().advanced.serverChannelID));
    }

    /**
     * Sends a message embed as player
     *
     * @param playerName Player Name
     * @param msg        Message
     * @param channel    Target channel
     * @param uuid       Player UUID
     */
    public void sendMessage(final String playerName, String uuid, String msg, TextChannel channel) {
        sendMessage(playerName, uuid, new DiscordMessage(msg), channel);
    }

    /**
     * Sends an generic message to discord with custom avatar url (when using a webhook)
     *
     * @param channel   target channel
     * @param message   message
     * @param avatarURL URL of the avatar image for the webhook
     * @param name      Webhook name
     */
    public void sendMessage(TextChannel channel, String message, String avatarURL, String name) {
        sendMessage(name, new DiscordMessage(message), avatarURL, channel, false);
    }

    /**
     * Sends a CHAT message to discord with custom avatar url (when using a webhook)
     *
     * @param msg       Message
     * @param avatarURL URL of the avatar image
     * @param name      Name of the fake player
     */
    public void sendMessage(String msg, String avatarURL, String name) {
        sendMessage(name, new DiscordMessage(msg), avatarURL, Configuration.instance().advanced.serverChannelID.equals("default") ? getChannel() : getChannel(Configuration.instance().advanced.serverChannelID), true);
    }

    /**
     * Sends a CHAT message to discord with custom avatar url (when using a webhook)
     *
     * @param name      Name of the fake player
     * @param msg       Message
     * @param channel   Channel to send message into
     * @param avatarURL URL of the avatar image
     */
    public void sendMessage(String name, String msg, TextChannel channel, String avatarURL) {
        sendMessage(name, new DiscordMessage(msg), avatarURL, channel, true);
    }

    /**
     * Sends an discord message
     *
     * @param name          Player name or Webhook user name
     * @param message       Message to send
     * @param avatarURL     Avatar URL for the webhook
     * @param channel       Target channel
     * @param isChatMessage true to send it as chat message (when not using webhook)
     */
    public void sendMessage(String name, DiscordMessage message, String avatarURL, TextChannel channel, boolean isChatMessage) {
        if (jda == null) return;
        try {
            if (Configuration.instance().webhook.enable) {
                final WebhookMessageBuilder b = message.buildWebhookMessage();
                b.setUsername(name);
                b.setAvatarUrl(avatarURL);
                getWebhookCli(channel.getId()).send(b.build());
            } else if (isChatMessage) {
                message.setMessage(Configuration.instance().localization.discordChatMessage.replace("%player%", name).replace("%msg%", message.getMessage()));
                channel.sendMessage(message.buildMessage()).queue();
            } else {
                channel.sendMessage(message.buildMessage()).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @return an instance of the webhook or null
     */
    @Nullable
    public Webhook getWebhook(TextChannel c) {
        if (!Configuration.instance().webhook.enable) return null;
        return webhookHashMap.computeIfAbsent(c.getId(), cid -> {
            if (!PermissionUtil.checkPermission(c, c.getGuild().getMember(jda.getSelfUser()), Permission.MANAGE_WEBHOOKS)) {
                Configuration.instance().webhook.enable = false;
                Configuration.instance().saveConfig();
                System.out.println("ERROR! Bot does not have permission to manage webhooks, disabling webhook");
                return null;
            }
            for (Webhook web : c.retrieveWebhooks().complete()) {
                if (web.getName().equals("MC_DISCORD_INTEGRATION")) {
                    return web;
                }
            }
            return c.createWebhook("MC_DISCORD_INTEGRATION").complete();
        });
    }

    /**
     * Sends a message when *not* using a webhook and returns it as RequestFuture<Message> or null when using a webhook<br>
     * only used by starting message
     *
     * @param msg message
     * @return Sent message
     */
    public CompletableFuture<Message> sendMessageReturns(String msg) {
        if (Configuration.instance().webhook.enable || msg.isEmpty()) return null;
        else return getChannel().sendMessage(msg).submit();
    }


    /**
     * Sends a message to discord
     *
     * @param msg         the message to send
     * @param textChannel the channel where the message should arrive
     */
    public void sendMessage(String msg, TextChannel textChannel) {
        sendMessage(new DiscordMessage(msg), textChannel);
    }

    /**
     * Sends a message to discord
     *
     * @param msg     the message to send
     * @param channel the channel where the message should arrive
     */
    public void sendMessage(DiscordMessage msg, TextChannel channel) {
        sendMessage(Configuration.instance().webhook.serverName, "0000000", msg, channel);
    }

    /**
     * Restarts the discord bot (used by reload command)
     */
    public boolean restart() {
        try {
            kill();
            if (Variables.discord_instance.isAlive()) Variables.discord_instance.interrupt();
            Variables.discord_instance = new Discord(srv);
            CommandRegistry.reRegisterAllCommands();
            CommandRegistry.registerConfigCommands();
            Variables.discord_instance.startThreads();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public HashMap<String, String> getSettings() {
        final HashMap<String, String> out = new HashMap<>();
        final Field[] fields = PlayerSettings.class.getFields();
        final Field[] descFields = PlayerSettings.Descriptions.class.getDeclaredFields();
        for (Field f : fields) {
            out.put(f.getName(), "No Description Provided");
        }
        for (Field f : descFields) {
            f.setAccessible(true);
            try {
                out.put(f.getName(), (String) f.get(new PlayerSettings.Descriptions()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return out;
    }


    /**
     * Sends a message to discord
     *
     * @param playerName the name of the player
     * @param uuid       the player uuid
     * @param msg        the message to send
     */
    @SuppressWarnings("ConstantConditions")
    public void sendMessage(String playerName, String uuid, DiscordMessage msg, TextChannel channel) {
        final boolean isServerMessage = playerName.equals(Configuration.instance().webhook.serverName) && uuid.equals("0000000");
        final UUID uUUID = uuid.equals("0000000") ? null : UUID.fromString(uuid);
        String avatarURL = "";
        if (!isServerMessage && uUUID != null) {
            if (PlayerLinkController.isPlayerLinked(uUUID)) {
                final PlayerSettings s = PlayerLinkController.getSettings(null, uUUID);
                final Member dc = channel.getGuild().getMemberById(PlayerLinkController.getDiscordFromPlayer(uUUID));
                if (s.useDiscordNameInChannel) {
                    playerName = dc.getEffectiveName();
                    avatarURL = dc.getUser().getAvatarUrl();
                }
            }
            if (avatarURL != null && avatarURL.isEmpty())
                avatarURL = "https://minotar.net/avatar/" + uuid;
        }
        if (isServerMessage) {
            avatarURL = Configuration.instance().webhook.serverAvatarURL;
        }
        sendMessage(playerName, msg, avatarURL, channel, !isServerMessage);
    }

    public boolean togglePlayerIgnore(UUID sender) {
        if (PlayerLinkController.isPlayerLinked(sender)) {
            final PlayerSettings settings = PlayerLinkController.getSettings(null, sender);
            settings.ignoreDiscordChatIngame = !settings.ignoreDiscordChatIngame;
            PlayerLinkController.updatePlayerSettings(null, sender, settings);
            return !settings.ignoreDiscordChatIngame;
        } else {
            if (ignoringPlayers.contains(sender)) {
                ignoringPlayers.remove(sender);
                try {
                    saveIgnoreList();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            } else {
                ignoringPlayers.add(sender);
                return false;
            }
        }
    }

    public int genLinkNumber(UUID uniqueID) {
        final AtomicInteger r = new AtomicInteger(-1);
        pendingLinks.forEach((k, v) -> {
            if (v.getValue().equals(uniqueID))
                r.set(k);
        });
        if (r.get() != -1) return r.get();
        do {
            r.set(new Random().nextInt(Integer.MAX_VALUE));
        } while (pendingLinks.containsKey(r.get()));
        pendingLinks.put(r.get(), new DefaultKeyValue<>(Instant.now(), uniqueID));
        return r.get();
    }

    private void saveIgnoreList() throws IOException {
        if (!IGNORED_PLAYERS.exists() && !ignoringPlayers.isEmpty()) IGNORED_PLAYERS.createNewFile();
        if (!IGNORED_PLAYERS.exists() && ignoringPlayers.isEmpty()) {
            IGNORED_PLAYERS.delete();
            return;
        }
        FileWriter w = new FileWriter(IGNORED_PLAYERS);
        w.write("");
        for (UUID a : ignoringPlayers) {
            if (!PlayerLinkController.isPlayerLinked(a))
                w.append(a.toString()).append("\n");
        }
        w.close();
    }


    private class MessageQueueThread extends Thread {
        MessageQueueThread() {
            setName("[Discord Integration] Message Queue");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                if (!messages.isEmpty()) {
                    messages.forEach((channel, msgs) -> {
                        StringBuilder s = new StringBuilder();
                        for (final String msg : msgs)
                            s.append(msg).append("\n");
                        Discord.this.sendMessage(s.toString().trim(), getChannel(channel));
                    });
                    messages.clear();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }

        }
    }

    private class StatusUpdateThread extends Thread {
        StatusUpdateThread() {
            setName("[Discord Integration] Discord status updater and link cleanup");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                if (jda != null) {
                    final String game = Configuration.instance().general.botStatusName
                            .replace("%online%", "" + srv.getOnlinePlayers())
                            .replace("%max%", "" + srv.getMaxPlayers());
                    switch (Configuration.instance().general.botStatusType) {
                        case DISABLED:
                            break;
                        case LISTENING:
                            jda.getPresence().setActivity(Activity.listening(game));
                            break;
                        case PLAYING:
                            jda.getPresence().setActivity(Activity.playing(game));
                            break;
                        case WATCHING:
                            jda.getPresence().setActivity(Activity.watching(game));
                            break;
                    }
                }
                // Removing of expired numbers
                final ArrayList<Integer> remove = new ArrayList<>();
                pendingLinks.forEach((k, v) -> {
                    final Instant now = Instant.now();
                    Duration d = Duration.between(v.getKey(), now);
                    if (d.toMinutes() > 10) remove.add(k);
                });
                for (int i : remove)
                    pendingLinks.remove(i);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }

            }
        }
    }

}
