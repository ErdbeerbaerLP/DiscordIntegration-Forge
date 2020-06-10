package de.erdbeerbaerlp.dcintegration;


import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.erdbeerbaerlp.dcintegration.api.DiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.commands.*;
import de.erdbeerbaerlp.dcintegration.storage.Configuration;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.teamfruit.emojicord.emoji.EmojiText;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;


@SuppressWarnings("ConstantConditions")
@Mod(DiscordIntegration.MODID)
public class DiscordIntegration {
    /**
     * Mod version
     */
    public static final String VERSION = "1.2.7";
    /**
     * Modid
     */
    public static final String MODID = "dcintegration";
    static final Logger LOGGER = LogManager.getLogger();
    /**
     * The only instance of {@link Discord}
     */
    public static Discord discord_instance;
    public static ArrayList<? extends DiscordEventHandler> eventHandlers = new ArrayList<>();
    /**
     * Time when the server was started
     */
    public static long started;
    public static ModConfig cfg = null;
    public static final ArrayList<UUID> timeouts = new ArrayList<>();
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
     * Message sent when the server is starting (in non-webhook mode!), stored for editing
     */
    private CompletableFuture<Message> startingMsg;
    /**
     * If the server was stopped or has crashed
     */
    private boolean stopped = false;

    public DiscordIntegration() throws SecurityException, IllegalArgumentException {

        final ModLoadingContext modLoadingContext = ModLoadingContext.get();
        modLoadingContext.registerConfig(ModConfig.Type.COMMON, Configuration.cfgSpec);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void registerConfigCommands() {
        final JsonObject commandJson = new JsonParser().parse(Configuration.INSTANCE.jsonCommands.get()).getAsJsonObject();
        LOGGER.info("Detected to load " + commandJson.size() + " commands to load from config");
        for (Map.Entry<String, JsonElement> cmd : commandJson.entrySet()) {
            final JsonObject cmdVal = cmd.getValue().getAsJsonObject();
            if (!cmdVal.has("mcCommand")) {
                LOGGER.warn("Skipping command " + cmd.getKey() + " because it is invalid! Check your config!");
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
                for (int i = 0; i < aliases.length; i++)
                    aliases[i] = cmdVal.getAsJsonArray("aliases").get(i).getAsString();
            }
            String[] channelID = (cmdVal.has("channelID") && cmdVal.get("channelID") instanceof JsonArray) ? Utils.makeStringArray(cmdVal.get("channelID").getAsJsonArray()) : new String[]{"0"};
            final DiscordCommand regCmd = new CommandFromCFG(cmd.getKey(), desc, mcCommand, admin, aliases, useArgs, argText, channelID);
            if (!discord_instance.registerCommand(regCmd))
                LOGGER.warn("Failed Registering command \"" + cmd.getKey() + "\" because it would override an existing command!");
        }
        LOGGER.info("Finished registering! Registered " + discord_instance.getCommandList().size() + " commands");
    }


    @SubscribeEvent
    public void playerJoin(final PlayerEvent.PlayerLoggedInEvent ev) {
        if (discord_instance != null) {
            discord_instance.sendMessage(Configuration.INSTANCE.msgPlayerJoin.get().replace("%player%", Utils.formatPlayerName(ev.getPlayer())));
        }
    }

    @SubscribeEvent
    public void onModConfigEvent(final ModConfig.ModConfigEvent event) {
        final ModConfig config = event.getConfig();
        // Rebake the configs when they change
        if (config.getSpec() == Configuration.cfgSpec) {
            cfg = config;
        }
    }

    @SubscribeEvent
    public void advancement(AdvancementEvent ev) {
        if (discord_instance != null && ev.getAdvancement().getDisplay() != null && ev.getAdvancement().getDisplay().shouldAnnounceToChat())
            discord_instance.sendMessage(Configuration.INSTANCE.msgAdvancement.get().replace("%player%",
                    TextFormatting.getTextWithoutFormattingCodes(ev.getPlayer()
                            .getName()
                            .getFormattedText()))
                    .replace("%name%",
                            TextFormatting.getTextWithoutFormattingCodes(ev.getAdvancement()
                                    .getDisplay()
                                    .getTitle()
                                    .getFormattedText()))
                    .replace("%desc%",
                            TextFormatting.getTextWithoutFormattingCodes(ev.getAdvancement()
                                    .getDisplay()
                                    .getDescription()
                                    .getFormattedText()))
                    .replace("\\n", "\n"));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void preInit(final FMLDedicatedServerSetupEvent ev) {
        LOGGER.info("Loading mod");
        try {
            discord_instance = new Discord();
            if (Configuration.INSTANCE.cmdHelpEnabled.get()) discord_instance.registerCommand(new CommandHelp());
            if (Configuration.INSTANCE.cmdListEnabled.get()) discord_instance.registerCommand(new CommandList());
            if (Configuration.INSTANCE.cmdUptimeEnabled.get()) discord_instance.registerCommand(new CommandUptime());
            registerConfigCommands();
            if (ModList.get().isLoaded("serverutilities")) {
                final File pdata = new File("./" + ev.getServerSupplier().get().getFolderName() + "/playerdata/" + Configuration.INSTANCE.senderUUID.get() + ".pdat");
                if (pdata.exists()) pdata.delete();
                else LOGGER.info("Generating playerdata file for comaptibility with ServerUtilities");
                pdata.createNewFile();
                final FileWriter w = new FileWriter(pdata);
                w.write("{\"uuid\":\"" + Configuration.INSTANCE.senderUUID.get() + "\",\"username\":\"DiscordFakeUser\",\"nickname\":\"Discord\",\"homes\":{},\"kitCooldowns\":{},\"perms\":[\"*\"],\"ranks\":[],\"maxHomes\":1,\"hasBeenRtpWarned\":false,\"enableFly\":false,\"isFlying\":false,\"godmode\":false,\"disableMsg\":false,\"firstKit\":false}");
                w.close();
            }
        } catch (Exception e) {
            LOGGER.fatal("Failed to login: " + e.getMessage());
            discord_instance = null;
        }
        if (discord_instance != null && !Configuration.INSTANCE.enableWebhook.get())
            if (!Configuration.INSTANCE.msgServerStarting.get().isEmpty())
                this.startingMsg = discord_instance.sendMessageReturns(Configuration.INSTANCE.msgServerStarting.get());
        if (discord_instance != null && Configuration.INSTANCE.botModifyDescription.get())
            (Configuration.INSTANCE.channelDescriptionID.get().isEmpty() ? discord_instance.getChannelManager() : discord_instance.getChannelManager(Configuration.INSTANCE.channelDescriptionID.get())).setTopic(Configuration.INSTANCE.descriptionStarting.get()).complete();
    }

    @SubscribeEvent
    public void serverAboutToStart(final FMLServerAboutToStartEvent ev) {

    }

    @SubscribeEvent
    public void serverStarting(final FMLServerStartingEvent ev) {
        new McCommandDiscord(ev.getCommandDispatcher());
    }

    @SubscribeEvent
    public void serverStarted(final FMLServerStartedEvent ev) {
        LOGGER.info("Started");
        //Detect mixin
        try {
            Class.forName("org.spongepowered.asm.mixin.Mixins");
        } catch (ClassNotFoundException e) {
            LOGGER.error("It looks like mixin is missing. Install MixinBootstrap from https://www.curseforge.com/minecraft/mc-mods/mixinbootstrap/files to get access to all features (whitelist and timeout detection)");
        }
        started = new Date().getTime();
        if (discord_instance != null) if (startingMsg != null) try {
            this.startingMsg.get().editMessage(Configuration.INSTANCE.msgServerStarted.get()).queue();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        else discord_instance.sendMessage(Configuration.INSTANCE.msgServerStarted.get());
        if (discord_instance != null) {
            discord_instance.startThreads();
        }
        Utils.runUpdateCheck();
    }

    @SubscribeEvent
    public void command(CommandEvent ev) {
        String command = ev.getParseResults().getReader().getString();
        if (discord_instance != null) {
            boolean raw = false;
            if (((command.startsWith("/say") || command.startsWith("say")) && Configuration.INSTANCE.sayOutput.get()) || ((command.startsWith("/me") || command.startsWith("me")) && Configuration.INSTANCE.meOutput.get())) {
                String msg = command.replace("/say ", "").replace("/me ", "");
                if (command.startsWith("say") || command.startsWith("me"))
                    msg = msg.replaceFirst("say ", "").replaceFirst("me ", "");
                if (command.startsWith("/me") || command.startsWith("me")) {
                    raw = true;
                    msg = "*" + Utils.escapeMarkdown(msg.trim()) + "*";
                }
                try {
                    discord_instance.sendMessage(ev.getParseResults().getContext().getSource().getName(), ev.getParseResults().getContext().getSource().assertIsEntity().getUniqueID().toString(), new Discord.DCMessage(null, msg, !raw), Configuration.INSTANCE.chatOutputChannel.get().isEmpty() ? discord_instance.getChannel() : discord_instance.getChannel(Configuration.INSTANCE.chatOutputChannel.get()));
                } catch (CommandSyntaxException e) {
                    if (msg.startsWith(Configuration.INSTANCE.sayCommandIgnoredPrefix.get())) return;
                    discord_instance.sendMessage(msg);
                }
            }
        }
    }

    private String getModNameFromID(String modid) {
        for (ModInfo c : ModList.get().getMods()) {
            if (c.getModId().equals(modid)) return c.getDisplayName();
        }
        return modid;
    }

    @SubscribeEvent
    public void serverStopping(final FMLServerStoppingEvent ev) {
        if (discord_instance != null) {
            discord_instance.stopThreads();
            if (Configuration.INSTANCE.enableWebhook.get() && !Configuration.INSTANCE.msgServerStopped.get().isEmpty()) {
                final WebhookMessageBuilder b = new WebhookMessageBuilder();
                b.setContent(Configuration.INSTANCE.msgServerStopped.get());
                b.setUsername(Configuration.INSTANCE.serverName.get());
                b.setAvatarUrl(Configuration.INSTANCE.serverAvatar.get());
                final WebhookClient cli = WebhookClient.withUrl(discord_instance.getWebhook(Configuration.INSTANCE.serverChannelID.get().isEmpty() ? discord_instance.getChannel() : discord_instance.getChannel(Configuration.INSTANCE.serverChannelID.get())).getUrl());
                cli.send(b.build());
                cli.close();
            } else if (!Configuration.INSTANCE.msgServerStopped.get().isEmpty())
                discord_instance.getChannel().sendMessage(Configuration.INSTANCE.msgServerStopped.get()).queue();
            if (Configuration.INSTANCE.botModifyDescription.get())
                (Configuration.INSTANCE.channelDescriptionID.get().isEmpty() ? discord_instance.getChannelManager() : discord_instance.getChannelManager(Configuration.INSTANCE.channelDescriptionID.get())).setTopic(Configuration.INSTANCE.descriptionOffline.get()).complete();
        }
        stopped = true;
    }

    @SubscribeEvent
    public void serverStopped(final FMLServerStoppedEvent ev) {
        if (discord_instance != null && !discord_instance.isKilled) {
            ev.getServer().runImmediately(() -> {
                if (!stopped) {
                    discord_instance.stopThreads();
                    if (Configuration.INSTANCE.botModifyDescription.get())
                        (Configuration.INSTANCE.channelDescriptionID.get().isEmpty() ? discord_instance.getChannelManager() : discord_instance.getChannelManager(Configuration.INSTANCE.channelDescriptionID.get())).setTopic(Configuration.INSTANCE.msgServerCrash.get()).complete();
                    discord_instance.getChannel().sendMessage(Configuration.INSTANCE.msgServerCrash.get()).submit();
                }
                discord_instance.kill();
            });
        }
    }


    //Untested
    @SubscribeEvent
    public void imc(InterModProcessEvent ev) {
        final Stream<InterModComms.IMCMessage> stream = ev.getIMCStream();
        stream.forEach((msg) -> {
            LOGGER.debug("[IMC-Message] Sender: " + msg.getSenderModId() + " method: " + msg.getMethod());
            if (isModIDBlacklisted(msg.getSenderModId())) return;
            if ((msg.getMethod().equals("Discord-Message") || msg.getMethod().equals("sendMessage"))) {
                discord_instance.sendMessage(msg.getMessageSupplier().get().toString());
            }
        });
    }

    private boolean isModIDBlacklisted(String sender) {
        return ArrayUtils.contains(Configuration.getArray(Configuration.INSTANCE.imcModIdBlacklist.get()), sender);
    }
    @SubscribeEvent
    public void chat(ServerChatEvent ev) {
        for (DiscordEventHandler o : DiscordIntegration.eventHandlers) {
            if (o.onMcChatMessage(ev)) return;
        }
        String text = Utils.escapeMarkdown(ev.getMessage().replace("@everyone", "[at]everyone").replace("@here", "[at]here"));
        if (ModList.get().isLoaded("emojicord")) {
            final EmojiText emojiText = EmojiText.create(text, EnumSet.of(EmojiText.ParseFlag.PARSE));
            for (EmojiText.EmojiTextElement emoji : emojiText.emojis) {
                if (emoji.id == null)
                    text = text.replace(emoji.raw, emoji.source);
                else
                    text = text.replace(emoji.raw, "<" + emoji.source
                            + emoji.id.getId() + ">");
            }
        }
        final MessageEmbed embed = Utils.genItemStackEmbedIfAvailable(ev.getComponent());
        if (discord_instance != null) {
            discord_instance.sendMessage(ev.getPlayer(), new Discord.DCMessage(embed, text, true));
        }
    }

    @SubscribeEvent
    public void death(LivingDeathEvent ev) {
        if (ev.getEntity() instanceof PlayerEntity || (ev.getEntity() instanceof TameableEntity && ((TameableEntity) ev.getEntity()).getOwner() instanceof PlayerEntity && Configuration.INSTANCE.tamedDeathEnabled.get())) {
            if (discord_instance != null) {
                final ITextComponent deathMessage = ev.getSource().getDeathMessage(ev.getEntityLiving());
                final MessageEmbed embed = Utils.genItemStackEmbedIfAvailable(deathMessage);
                discord_instance.sendMessage(new Discord.DCMessage(embed, Configuration.INSTANCE.msgPlayerDeath.get().replace("%player%", Utils.formatPlayerName(ev.getEntity())).replace("%msg%", TextFormatting.getTextWithoutFormattingCodes(deathMessage.getFormattedText()).replace(ev.getEntity().getName().getUnformattedComponentText() + " ", ""))), Configuration.INSTANCE.deathChannelID.get().isEmpty() ? discord_instance.getChannel() : discord_instance.getChannel(Configuration.INSTANCE.deathChannelID.get()));
            }
        }
    }

    @SubscribeEvent
    public void playerLeave(PlayerEvent.PlayerLoggedOutEvent ev) {
        if (discord_instance != null && !timeouts.contains(ev.getPlayer().getUniqueID()))
            discord_instance.sendMessage(Configuration.INSTANCE.msgPlayerLeave.get().replace("%player%", Utils.formatPlayerName(ev.getPlayer())));
        else if (discord_instance != null && timeouts.contains(ev.getPlayer().getUniqueID())) {
            discord_instance.sendMessage(Configuration.INSTANCE.msgPlayerTimeout.get().replace("%player%", Utils.formatPlayerName(ev.getPlayer())));
            timeouts.remove(ev.getPlayer().getUniqueID());
        }

    }
}
