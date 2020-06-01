package de.erdbeerbaerlp.dcintegration;


import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftbutilities.data.FTBUtilitiesPlayerData;
import com.google.gson.*;
import de.erdbeerbaerlp.dcintegration.commands.*;
import net.dv8tion.jda.api.entities.Message;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.ForgeVersion.CheckResult;
import net.minecraftforge.common.ForgeVersion.Status;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static de.erdbeerbaerlp.dcintegration.Configuration.ADVANCED;


@Mod(modid = DiscordIntegration.MODID, version = DiscordIntegration.VERSION, name = DiscordIntegration.NAME, serverSideOnly = true, acceptableRemoteVersions = "*",
     updateJSON = "https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/master/update_check.json")
public class DiscordIntegration
{
    /**
     * Mod name
     */
    public static final String NAME = "Discord Integration";
    /**
     * Mod version
     */
    public static final String VERSION = "1.1.18";
    /**
     * Modid
     */
    public static final String MODID = "dcintegration";
    public static final ArrayList<UUID> timeouts = new ArrayList<>();
    /**
     * The only instance of {@link Discord}
     */
    @Nullable
    public static Discord discord_instance;
    /**
     * Time when the server was started
     */
    public static long started;
    /**
     * Message sent when the server is starting (in non-webhook mode!), stored for editing
     */
    private CompletableFuture<Message> startingMsg;
    /**
     * If the server was stopped or has crashed
     */
    private boolean stopped = false;
    
    public DiscordIntegration() {}
    
    static String defaultCommandJson;
    
    static {
        final JsonObject a = new JsonObject();
        final JsonObject kick = new JsonObject();
        kick.addProperty("adminOnly", true);
        kick.addProperty("mcCommand", "kick");
        kick.addProperty("description", "Kicks a player from the server");
        kick.addProperty("useArgs", true);
        kick.addProperty("argText", "<player> [reason]");
        a.add("kick", kick);
        final JsonObject stop = new JsonObject();
        stop.addProperty("adminOnly", true);
        stop.addProperty("mcCommand", "stop");
        stop.addProperty("description", "Stops the server");
        final JsonArray stopAliases = new JsonArray();
        stopAliases.add("shutdown");
        stop.add("aliases", stopAliases);
        stop.addProperty("useArgs", false);
        a.add("stop", stop);
        final JsonObject kill = new JsonObject();
        kill.addProperty("adminOnly", true);
        kill.addProperty("mcCommand", "kill");
        kill.addProperty("description", "Kills a player");
        kill.addProperty("useArgs", true);
        kill.addProperty("argText", "<player>");
        a.add("kill", kill);
        final JsonObject tps = new JsonObject();
        tps.addProperty("adminOnly", false);
        tps.addProperty("mcCommand", "forge tps");
        tps.addProperty("description", "Displays TPS");
        tps.addProperty("useArgs", false);
        a.add("tps", tps);
        final Gson gson = new GsonBuilder().create();
        defaultCommandJson = gson.toJson(a);
    }
    /**
     * Removes Color code formatting
     *
     * @param formatted Formatted text with §2 color codes
     * @return Raw text without color codes
     */
    public static String removeFormatting(String formatted) {
        return formatted.replaceAll("\u00A70", "").replaceAll("\u00A71", "").replaceAll("\u00A72", "").replaceAll("\u00A73", "").replaceAll("\u00A74", "").replaceAll("\u00A75", "").replaceAll("\u00A76", "").replaceAll("\u00A77", "")
                        .replaceAll("\u00A78", "").replaceAll("\u00A79", "").replaceAll("\u00A7a", "").replaceAll("\u00A7b", "").replaceAll("\u00A7c", "").replaceAll("\u00A7d", "").replaceAll("\u00A7e", "").replaceAll("\u00A7f", "")
                        .replaceAll("\u00A7l", "").replaceAll("\u00A7k", "").replaceAll("\u00A7m", "").replaceAll("\u00A7n", "").replaceAll("\u00A7o", "").replaceAll("\u00A7r", "");
    }
    
    public static String formatPlayerName(Entity p, boolean chatFormat) {
        if (Loader.isModLoaded("ftbutilities") && p instanceof EntityPlayer) {
            final FTBUtilitiesPlayerData d = FTBUtilitiesPlayerData.get(Universe.get().getPlayer(p));
            final String nick = (Configuration.FTB_UTILITIES.CHAT_FORMATTING && chatFormat) ? d.getNameForChat((EntityPlayerMP) p).getUnformattedText().replace("<", "").replace(">", "").trim() : d.getNickname().trim();
            if (!nick.isEmpty()) return nick;
        }
        return p.getName();
    }

    public static String formatPlayerName(Entity p) {
        return formatPlayerName(p, true);
    }

    public static String getFullUptime() {
        if (started == 0) {
            return "?????";
        }
        final Duration duration = Duration.between(Instant.ofEpochMilli(started), Instant.now());
        return DurationFormatUtils.formatDuration(duration.toMillis(), Configuration.MESSAGES.UPTIME_FORMAT);
    }

    public static int getUptimeSeconds() {
        long diff = new Date().getTime() - started;
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

    public static void registerConfigCommands() {
        final JsonObject commandJson = new JsonParser().parse(Configuration.COMMANDS.JSON_COMMANDS).getAsJsonObject();
        System.out.println("Detected to load " + commandJson.size() + " commands to load from config");
        for (Map.Entry<String, JsonElement> cmd : commandJson.entrySet()) {
            final JsonObject cmdVal = cmd.getValue().getAsJsonObject();
            if (!cmdVal.has("mcCommand")) {
                System.err.println("Skipping command " + cmd.getKey() + " because it is invalid! Check your config!");
                continue;
            }
            final String mcCommand = cmdVal.get("mcCommand").getAsString();
            final String desc = cmdVal.has("description") ? cmdVal.get("description").getAsString() : "No Description";
            final boolean admin = !cmdVal.has("adminOnly") || cmdVal.get("adminOnly").getAsBoolean();
            final boolean useArgs = !cmdVal.has("useArgs") || cmdVal.get("useArgs").getAsBoolean();
            String argText = "<args>";
            if (cmdVal.has("argText")) argText = cmdVal.get("argText").getAsString();
            String[] aliases = new String[0];
            if (cmdVal.has("aliases") && cmdVal.get("aliases").isJsonArray()) {
                aliases = new String[cmdVal.getAsJsonArray("aliases").size()];
                for (int i = 0 ; i < aliases.length ; i++)
                    aliases[i] = cmdVal.getAsJsonArray("aliases").get(i).getAsString();
            }
            String[] channelID = (cmdVal.has("channelID") && cmdVal.get("channelID") instanceof JsonArray) ? makeStringArray(cmdVal.get("channelID").getAsJsonArray()) : new String[]{"0"};
            final DiscordCommand regCmd = new CommandFromCFG(cmd.getKey(), desc, mcCommand, admin, aliases, useArgs, argText, channelID);
            if (!discord_instance.registerCommand(regCmd)) System.err.println("Failed Registering command \"" + cmd.getKey() + "\" because it would override an existing command!");
    
        }
    }
    
    private static String[] makeStringArray(final JsonArray channelID) {
        final String[] out = new String[channelID.size()];
        for (int i = 0 ; i < out.length ; i++) {
            out[i] = channelID.get(i).getAsString();
        }
        return out;
    }
    
    @EventHandler
    public void init(FMLInitializationEvent ev) {
        if (discord_instance != null && !Configuration.WEBHOOK.BOT_WEBHOOK)
            this.startingMsg = discord_instance.sendMessageReturns(Configuration.MESSAGES.SERVER_STARTING_MSG);
        if (discord_instance != null && Configuration.GENERAL.MODIFY_CHANNEL_DESCRIPTRION)
            (ADVANCED.CHANNEL_DESCRIPTION_ID.isEmpty() ? discord_instance.getChannelManager() : discord_instance.getChannelManager(ADVANCED.CHANNEL_DESCRIPTION_ID)).setTopic(Configuration.MESSAGES.CHANNEL_DESCRIPTION_STARTING).complete();

    }
    
    @EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent ev) {
    
    }
    
    @EventHandler
    public void serverStarting(FMLServerStartingEvent ev) {
        ev.registerServerCommand(new McCommandDiscord());
    }
    
    @EventHandler
    public void serverStarted(FMLServerStartedEvent ev) {
        started = new Date().getTime();
        if (discord_instance != null && !Configuration.MESSAGES.SERVER_STARTED_MSG.isEmpty()) try {
            if (startingMsg != null)
                this.startingMsg.get().editMessage(Configuration.MESSAGES.SERVER_STARTED_MSG).queue();
            else discord_instance.sendMessage(Configuration.MESSAGES.SERVER_STARTED_MSG);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (discord_instance != null) {
            discord_instance.startThreads();
        }
        final Thread discordShutdownThread = new Thread(this::stopDiscord);
        discordShutdownThread.setDaemon(false);
        //Runtime.getRuntime().addShutdownHook(discordShutdownThread);
        
        if (Configuration.GENERAL.UPDATE_CHECK) {
            CheckResult result = ForgeVersion.getResult(Loader.instance().getIndexedModList().get(DiscordIntegration.MODID));
            if (result.status == Status.OUTDATED) {
                System.out.println(
                        "\n\u00A76[\u00A75DiscordIntegration\u00A76]\u00A7c Update available!\n\u00A7cCurrent version: \u00A74" + DiscordIntegration.VERSION + "\u00A7c, Newest: \u00A7a" + result.target + "\n\u00A7cChangelog:\n\u00A76" + result.changes
                                .get(result.target) + "\nDownload the newest version on https://minecraft.curseforge.com/projects/dcintegration");
            }
            else if (result.status == Status.AHEAD) {
                System.out.println("\n\u00A76[\u00A75DiscordIntegration\u00A76]\u00A77 It looks like you are using an Development version... \n\u00A77Your version: \u00A76" + DiscordIntegration.VERSION);
            }
            else if (result.status == Status.FAILED) {
                System.err.println("\u00A76[\u00A75DiscordIntegration\u00A76]\u00A7c FAILED TO CHECK FOR UPDATES");
            }
            else if (result.status == Status.BETA) {
                System.out.println("\n\u00A76[\u00A75DiscordIntegration\u00A76]\u00A7a You are using an Beta Version. This may contain bugs which are being fixed.");
            }
            else if (result.status == Status.BETA_OUTDATED) {
                System.out.println(new TextComponentString(
                        "\u00A76[\u00A75DiscordIntegration\u00A76]\u00A7c You are using an Outdated Beta Version. This may contain bugs which are being fixed or are already fixed\n\u00A76Changelog of newer Beta:" + result.changes
                                .get(result.target)));
            }
            else //noinspection StatementWithEmptyBody
                if (result.status == Status.UP_TO_DATE) {
                }
                else {
                    System.out.println("\n\u00A76[\u00A75DiscordIntegration\u00A76]\u00A7cUpdateCheck: " + result.status.toString());
                }
            
        }
    }
    
    @EventHandler
    public void imc(FMLInterModComms.IMCEvent ev) {
        for (FMLInterModComms.IMCMessage e : ev.getMessages()) {
            System.out.println("[IMC-Message] Sender: " + e.getSender() + "Key: " + e.key);
            if (isModIDBlacklisted(e.getSender())) continue;
            if (e.isStringMessage() && (e.key.equals("Discord-Message") || e.key.equals("sendMessage"))) {
                discord_instance.sendMessage(e.getStringValue());
            }
            //Compat with imc from another discord integration mod
            if (e.isNBTMessage() && e.key.equals("sendMessage")) {
                final NBTTagCompound msg = e.getNBTValue();
                discord_instance.sendMessage(msg.getString("message"));
            }
        }
    }
    
    private boolean isModIDBlacklisted(String sender) {
        return ArrayUtils.contains(Configuration.COMMANDS.IMC_MOD_ID_BLACKLIST, sender);
    }
    
    @EventHandler
    public void serverStopping(FMLServerStoppingEvent ev) {
        if (discord_instance != null) {
            discord_instance.stopThreads();
            if (Configuration.WEBHOOK.BOT_WEBHOOK) {
                final WebhookMessageBuilder b = new WebhookMessageBuilder();
                b.setContent(Configuration.MESSAGES.SERVER_STOPPED_MSG);
                b.setUsername(Configuration.WEBHOOK.SERVER_NAME);
                b.setAvatarUrl(Configuration.WEBHOOK.SERVER_AVATAR);
                @SuppressWarnings("ConstantConditions") final WebhookClient cli = WebhookClient.withUrl(discord_instance.getWebhook(ADVANCED.SERVER_CHANNEL_ID.isEmpty() ? discord_instance.getChannel() : discord_instance.getChannel(ADVANCED.SERVER_CHANNEL_ID)).getUrl());
                cli.send(b.build());
                cli.close();
            } else if (!Configuration.MESSAGES.SERVER_STOPPED_MSG.isEmpty())
                discord_instance.getChannel().sendMessage(Configuration.MESSAGES.SERVER_STOPPED_MSG).complete();
            if (Configuration.GENERAL.MODIFY_CHANNEL_DESCRIPTRION)
                (ADVANCED.CHANNEL_DESCRIPTION_ID.isEmpty() ? discord_instance.getChannelManager() : discord_instance.getChannelManager(ADVANCED.CHANNEL_DESCRIPTION_ID)).setTopic(Configuration.MESSAGES.CHANNEL_DESCRIPTION_OFFLINE).complete();
        }
        stopped = true;
    }
    
    @EventHandler
    public void serverStopped(FMLServerStoppedEvent ev) {
        FMLCommonHandler.instance().getMinecraftServerInstance().callFromMainThread(Executors.callable(this::stopDiscord)); //Attempt to force send the messages before server finally closes
    }
    
    private void stopDiscord() {
        if (discord_instance != null) {
            if (!stopped) {
                if (!discord_instance.isKilled) {
                    discord_instance.stopThreads();
                    discord_instance.sendMessage(Configuration.MESSAGES.SERVER_CRASHED_MSG);
                    if (Configuration.GENERAL.MODIFY_CHANNEL_DESCRIPTRION)
                        (ADVANCED.CHANNEL_DESCRIPTION_ID.isEmpty() ? discord_instance.getChannelManager() : discord_instance.getChannelManager(ADVANCED.CHANNEL_DESCRIPTION_ID)).setTopic(Configuration.MESSAGES.SERVER_CRASHED_MSG).complete();
                }
            }
            discord_instance.kill();
        }
    }
    
    @SubscribeEvent
    public void playerJoin(PlayerLoggedInEvent ev) {
        if (discord_instance != null && !Configuration.MESSAGES.DISABLE_JOIN_LEAVE_MESSAGES)
            discord_instance.sendMessage(Configuration.MESSAGES.PLAYER_JOINED_MSG.replace("%player%", formatPlayerName(ev.player, false)));
    }
    
    @SubscribeEvent
    public void playerLeave(PlayerLoggedOutEvent ev) {
        if (discord_instance != null && !Configuration.MESSAGES.DISABLE_JOIN_LEAVE_MESSAGES && !timeouts.contains(ev.player.getUniqueID()))
            discord_instance.sendMessage(Configuration.MESSAGES.PLAYER_LEFT_MSG.replace("%player%", ev.player.getName()));
        else if (discord_instance != null && timeouts.contains(ev.player.getUniqueID())) {
            discord_instance.sendMessage(Configuration.MESSAGES.PLAYER_TIMEOUT_MSG.replace("%player%", ev.player.getName()));
            timeouts.remove(ev.player.getUniqueID());
        }
    }
    
    @SuppressWarnings("StringConcatenationInLoop")
    @SubscribeEvent
    public void command(CommandEvent ev) {
        //Uncomment below to crash the server on console command (used to test crash detection)
        //((World)null).setBlockState(null, null);
        if (ev.isCanceled()) return;
        if (discord_instance != null) if ((ev.getCommand().getName().equals("say") && Configuration.MESSAGES.ENABLE_SAY_OUTPUT) || (ev.getCommand().getName().equals("me") && Configuration.MESSAGES.ENABLE_ME_OUTPUT)) {
            String msg = "";
            for (String s : ev.getParameters()) {
                msg = msg + s + " ";
            }
            if (ev.getCommand().getName().equals("me")) msg = "*" + msg.trim() + "*";
            if (ev.getSender() instanceof DedicatedServer) discord_instance.sendMessage(msg);
            else if (ev.getSender() instanceof EntityPlayer || ev.getSender() instanceof FakePlayer) discord_instance.sendMessage(formatPlayerName((EntityPlayer) ev.getSender()),
                                                                                                                                  ev.getSender().getCommandSenderEntity().getUniqueID().toString(), msg);
        }
    }
    
    @SubscribeEvent
    public void chat(ServerChatEvent ev) {
        if (discord_instance != null) discord_instance.sendMessage(ev.getPlayer(), ev.getMessage().replace("@everyone", "[at]everyone").replace("@here", "[at]here"));
    }
    
    @SubscribeEvent
    public void death(LivingDeathEvent ev) {
        if (ev.getEntity() instanceof EntityPlayerMP || (ev.getEntity() instanceof EntityTameable && ((EntityTameable) ev.getEntity()).getOwner() instanceof EntityPlayerMP && Configuration.MESSAGES.TAMED_DEATH_ENABLED)) {
            if (discord_instance != null)
                discord_instance.sendMessage(Configuration.MESSAGES.PLAYER_DEATH_MSG.replace("%player%", formatPlayerName(ev.getEntity())).replace("%msg%", ev.getSource().getDeathMessage(ev.getEntityLiving())
                        .getUnformattedText()
                        .replace(ev.getEntity().getName() + " ", "")), ADVANCED.DEATH_CHANNEL_ID.isEmpty() ? discord_instance.getChannel() : discord_instance.getChannel(ADVANCED.DEATH_CHANNEL_ID));
        }
    }
    
    private static final Pattern PATTERN_CONTROL_CODE = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
    
    public static String stripControlCodes(String text) {
        return PATTERN_CONTROL_CODE.matcher(text).replaceAll("");
    }
    @SubscribeEvent
    public void advancement(AdvancementEvent ev) {
        if (discord_instance != null && ev.getAdvancement().getDisplay() != null && ev.getAdvancement().getDisplay().shouldAnnounceToChat()) discord_instance.sendMessage(Configuration.MESSAGES.PLAYER_ADVANCEMENT_MSG.replace("%player%",
                                                                                                                                                                                                                                formatPlayerName(
                                                                                                                                                                                                                                        ev.getEntityPlayer()))
                                                                                                                                                                                                                       .replace("%name%",
                                                                                                                                                                                                                                ev.getAdvancement()
                                                                                                                                                                                                                                  .getDisplay()
                                                                                                                                                                                                                                  .getTitle()
                                                                                                                                                                                                                                  .getUnformattedText())
                                                                                                                                                                                                                       .replace("%desc%",
                                                                                                                                                                                                                                ev.getAdvancement()
                                                                                                                                                                                                                                  .getDisplay()
                                                                                                                                                                                                                                  .getDescription()
                                                                                                                                                                                                                                  .getUnformattedText())
                                                                                                                                                                                                                       .replace("\\n", "\n"));
    }
    @EventHandler
    public void preInit(FMLPreInitializationEvent ev) {
        System.out.println("Loading mod");
        try {
            discord_instance = new Discord();
            System.out.println("Registering discord commands...");
            if (Configuration.COMMANDS.ENABLE_HELP_COMMAND) discord_instance.registerCommand(new CommandHelp());
            if (Configuration.COMMANDS.ENABLE_UPTIME_COMMAND) discord_instance.registerCommand(new CommandUptime());
            if (Configuration.COMMANDS.ENABLE_LIST_COMMAND) discord_instance.registerCommand(new CommandList());
            registerConfigCommands();
            System.out.println("Finished registering! Registered " + discord_instance.getCommandList().size() + " commands");
        } catch (Exception e) {
            System.err.println("Failed to login: " + e.getMessage());
            discord_instance = null;
        }
        MinecraftForge.EVENT_BUS.register(this);
        if (Loader.isModLoaded("votifier")) {
            MinecraftForge.EVENT_BUS.register(new VotifierEventHandler());
        }
    }
}
