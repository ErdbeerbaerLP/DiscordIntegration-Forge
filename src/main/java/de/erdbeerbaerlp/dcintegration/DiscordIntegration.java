package de.erdbeerbaerlp.dcintegration;


import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.erdbeerbaerlp.dcintegration.commands.*;
import net.dv8tion.jda.api.entities.Message;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.server.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@SuppressWarnings("ConstantConditions")
@Mod(DiscordIntegration.MODID)
public class DiscordIntegration
{
    /**
     * Mod name
     */
    public static final String NAME = "Discord Integration";
    /**
     * Mod version
     */
    public static final String VERSION = "1.1.2";
    /**
     * Modid
     */
    public static final String MODID = "dcintegration";
    private static final Logger LOGGER = LogManager.getLogger();
    /**
     * The only instance of {@link Discord}
     */
    public static Discord discord_instance;
    /**
     * Time when the server was started
     */
    public static long started;
    public static ModConfig cfg = null;
    public static PlayerEntity lastTimeout;
    /**
     * Message sent when the server is starting (in non-webhook mode!), stored for editing
     */
    private CompletableFuture<Message> startingMsg;
    /**
     * If the server was stopped or has crashed
     */
    private boolean stopped = false;
    
    public DiscordIntegration() throws SecurityException, IllegalArgumentException {
        
        //Register Config
        final ModLoadingContext modLoadingContext = ModLoadingContext.get();
        modLoadingContext.registerConfig(ModConfig.Type.COMMON, Configuration.cfgSpec);
        
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
        
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
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
    
    public static String formatPlayerName(Entity p) {
        return formatPlayerName(p, true);
    }
    
    public static void registerConfigCommands() {
        final JsonObject commandJson = new JsonParser().parse(Configuration.INSTANCE.jsonCommands.get()).getAsJsonObject();
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
            final DiscordCommand regCmd = new CommandFromCFG(cmd.getKey(), desc, mcCommand, admin, aliases, useArgs, argText);
            if (!discord_instance.registerCommand(regCmd)) System.err.println("Failed Registering command \"" + cmd.getKey() + "\" because it would override an existing command!");
            
        }
    }
    
    public static String getUptime() {
        if (started == 0) {
            return "?????";
        }
        
        long diff = new Date().getTime() - started;
        
        int seconds = (int) Math.floorDiv(diff, 1000);
        if (seconds < 60) {
            return seconds + " second" + (seconds == 1 ? "" : "s");
        }
        int minutes = Math.floorDiv(seconds, 60);
        seconds -= minutes * 60;
        if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + ", " + seconds + " second" + (seconds == 1 ? "" : "s");
        }
        int hours = Math.floorDiv(minutes, 60);
        minutes -= hours * 60;
        if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + ", " + minutes + " minute" + (minutes == 1 ? "" : "s") + ", " + seconds + " second" + (seconds == 1 ? "" : "s");
        }
        int days = Math.floorDiv(hours, 24);
        hours -= days * 24;
        return days + " day" + (days == 1 ? "" : "s") + ", " + hours + " hour" + (hours == 1 ? "" : "s") + ", " + minutes + " minute" + (minutes == 1 ? "" : "s") + ", " + seconds + " second" + (seconds == 1 ? "" : "s");
    }
    
    public static String formatPlayerName(Entity p, boolean chatFormat) {
        /*if (Loader.isModLoaded("ftbutilities") && p instanceof EntityPlayer) {
            final FTBUtilitiesPlayerData d = FTBUtilitiesPlayerData.get(Universe.get().getPlayer(p));
            final String nick = (Configuration.FTB_UTILITIES.CHAT_FORMATTING && chatFormat) ? d.getNameForChat((EntityPlayerMP) p).getUnformattedText().replace("<", "").replace(">", "").trim() : d.getNickname().trim();
            if (!nick.isEmpty()) return nick;
        }*/
        return p.getName().getUnformattedComponentText();
    }
    
    @SubscribeEvent
    public void playerJoin(final PlayerEvent.PlayerLoggedInEvent ev) {
        if (discord_instance != null) discord_instance.sendMessage(Configuration.INSTANCE.msgPlayerJoin.get().replace("%player%", ev.getPlayer().getName().getUnformattedComponentText()));
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
    public void chat(ServerChatEvent ev) {
        if (discord_instance != null) discord_instance.sendMessage(ev.getPlayer(), ev.getMessage().replace("@everyone", "[at]everyone").replace("@here", "[at]here"));
    }
    
    @SubscribeEvent
    public void death(LivingDeathEvent ev) {
        if (ev.getEntity() instanceof PlayerEntity || (ev.getEntity() instanceof TameableEntity && ((TameableEntity) ev.getEntity()).getOwner() instanceof PlayerEntity && Configuration.INSTANCE.tamedDeathEnabled.get())) {
            if (discord_instance != null) discord_instance.sendMessage(Configuration.INSTANCE.msgPlayerDeath.get().replace("%player%", formatPlayerName(ev.getEntity())).replace("%msg%", ev.getSource().getDeathMessage(ev.getEntityLiving())
                                                                                                                                                                                            .getUnformattedComponentText().replace(
                            ev.getEntity().getName().getUnformattedComponentText() + " ", "")));
        }
    }
    
    @SubscribeEvent
    public void advancement(AdvancementEvent ev) {
        if (discord_instance != null && ev.getAdvancement().getDisplay() != null && ev.getAdvancement().getDisplay().shouldAnnounceToChat()) discord_instance.sendMessage(Configuration.INSTANCE.msgAdvancement.get().replace("%player%",
                                                                                                                                                                                                                              ev.getPlayer()
                                                                                                                                                                                                                                .getName()
                                                                                                                                                                                                                                .getUnformattedComponentText())
                                                                                                                                                                                                               .replace("%name%",
                                                                                                                                                                                                                        ev.getAdvancement()
                                                                                                                                                                                                                          .getDisplay()
                                                                                                                                                                                                                          .getTitle()
                                                                                                                                                                                                                          .getUnformattedComponentText())
                                                                                                                                                                                                               .replace("%desc%",
                                                                                                                                                                                                                        ev.getAdvancement()
                                                                                                                                                                                                                          .getDisplay()
                                                                                                                                                                                                                          .getDescription()
                                                                                                                                                                                                                          .getUnformattedComponentText())
                                                                                                                                                                                                               .replace("\\n", "\n"));
    }
    
    public void preInit(final FMLDedicatedServerSetupEvent ev) {
        System.out.println("Loading mod");
        try {
            discord_instance = new Discord();
            discord_instance.registerCommand(new CommandHelp());
            discord_instance.registerCommand(new CommandList());
            discord_instance.registerCommand(new CommandUptime());
            final JsonObject commandJson = new JsonParser().parse(Configuration.INSTANCE.jsonCommands.get()).getAsJsonObject();
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
                if (!discord_instance.registerCommand(new CommandFromCFG(cmd.getKey(), desc, mcCommand, admin, aliases, useArgs, argText))) System.err.println(
                        "Failed Registering command \"" + cmd.getKey() + "\" because it would override an existing command!");
            }
            System.out.println("Finished registering! Registered " + discord_instance.getCommandList().size() + " commands");
        } catch (Exception e) {
            System.err.println("Failed to login: " + e.getMessage());
            discord_instance = null;
        }
        if (discord_instance != null && !Configuration.INSTANCE.enableWebhook.get()) this.startingMsg = discord_instance.sendMessageReturns("Server Starting...");
        if (discord_instance != null && Configuration.INSTANCE.botModifyDescription.get()) discord_instance.getChannelManager().setTopic(Configuration.INSTANCE.descriptionStarting.get()).complete();
    
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
        /*Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (discord_instance != null) {
                if (!stopped) {
                    if (!discord_instance.isKilled) {
                        discord_instance.stopThreads();
                        if (Configuration.INSTANCE.botModifyDescription.get()) discord_instance.getChannelManager().setTopic(Configuration.INSTANCE.msgServerCrash.get()).complete();
                        discord_instance.sendMessage(Configuration.INSTANCE.msgServerCrash.get());
                    }
                }
                discord_instance.kill();
            }
        }));*/
        
        //noinspection StatementWithEmptyBody
        if (Configuration.INSTANCE.updateCheck.get()) {
            //UNUSED for now
            //				CheckResult result = ForgeVersion.getResult(FML.instance().getIndexedModList().get(DiscordIntegration.MODID));
            //				if (result.status == Status.OUTDATED){
            //					System.out.println("\n\u00A76[\u00A75DiscordIntegration\u00A76]\u00A7c Update available!\n\u00A7cCurrent version: \u00A74"+DiscordIntegration.VERSION+"\u00A7c, Newest: \u00A7a"+result.target+"\n\u00A7cChangelog:\n\u00A76"+result.changes.get(result.target)+"\nDownload the newest version on https://minecraft.curseforge.com/projects/dcintegration");
            //				}else if(result.status == Status.AHEAD){
            //					System.out.println("\n\u00A76[\u00A75DiscordIntegration\u00A76]\u00A77 It looks like you are using an Development version... \n\u00A77Your version: \u00A76"+DiscordIntegration.VERSION);
            //				}else if(result.status == Status.FAILED){
            //					System.err.println("\u00A76[\u00A75DiscordIntegration\u00A76]\u00A7c FAILED TO CHECK FOR UPDATES");
            //				}else if(result.status == Status.BETA){
            //					System.out.println("\n\u00A76[\u00A75DiscordIntegration\u00A76]\u00A7a You are using an Beta Version. This may contain bugs which are being fixed.");
            //				}else if(result.status == Status.BETA_OUTDATED){
            //					System.out.println("\u00A76[\u00A75DiscordIntegration\u00A76]\u00A7c You are using an Outdated Beta Version. This may contain bugs which are being fixed or are already fixed\n\u00A76Changelog of newer Beta:"+result.changes.get(result.target));
            //				}else if(result.status == Status.UP_TO_DATE) {
            //				}else {
            //					System.out.println("\n\u00A76[\u00A75DiscordIntegration\u00A76]\u00A7cUpdateCheck: "+result.status.toString());
            //				}
        }
    }
    
    @SubscribeEvent
    public void command(CommandEvent ev) {
        String command = ev.getParseResults().getReader().getString();
        if (discord_instance != null) {
            if (((command.startsWith("/say") || command.startsWith("say")) && Configuration.INSTANCE.sayOutput.get()) || ((command.startsWith("/me") || command.startsWith("me")) && Configuration.INSTANCE.meOutput.get())) {
                String msg = command.replace("/say ", "").replace("/me ", "");
                if (command.startsWith("say") || command.startsWith("me")) msg = msg.replaceFirst("say ", "").replaceFirst("me ", "");
                if (command.startsWith("/me") || command.startsWith("me")) msg = "*" + msg.trim() + "*";
                try {
                    discord_instance.sendMessage(ev.getParseResults().getContext().getSource().getName(), ev.getParseResults().getContext().getSource().assertIsEntity().getUniqueID().toString(), msg);
                } catch (CommandSyntaxException e) {
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
            if (Configuration.INSTANCE.enableWebhook.get()) {
                final WebhookMessageBuilder b = new WebhookMessageBuilder();
                b.setContent(Configuration.INSTANCE.msgServerStopped.get());
                b.setUsername(Configuration.INSTANCE.serverName.get());
                b.setAvatarUrl(Configuration.INSTANCE.serverAvatar.get());
                final WebhookClient cli = WebhookClient.withUrl(discord_instance.getWebhook().getUrl());
                cli.send(b.build());
                cli.close();
            }
            else discord_instance.getChannel().sendMessage(Configuration.INSTANCE.msgServerStopped.get()).complete();
            if (Configuration.INSTANCE.botModifyDescription.get()) discord_instance.getChannelManager().setTopic(Configuration.INSTANCE.descriptionOffline.get()).complete();
        }
        stopped = true;
    }
    
    @SubscribeEvent
    public void serverStopped(final FMLServerStoppedEvent ev) {
        ev.getServer().runImmediately(() -> {
            if (discord_instance != null) {
            if (!stopped) {
                if (!discord_instance.isKilled) {
                    discord_instance.stopThreads();
                    if (Configuration.INSTANCE.botModifyDescription.get()) discord_instance.getChannelManager().setTopic(Configuration.INSTANCE.msgServerCrash.get()).complete();
                    discord_instance.sendMessage(Configuration.INSTANCE.msgServerCrash.get());
                }
            }
            discord_instance.kill();
            }
        });
       
    }
    
    /* TODO Find out more
    @SubscribeEvent
    public void imc(InterModEnqueueEvent ev) {
        for (InterModComms.IMCMessage e : ev.getMessages()) {
            System.out.println("[IMC-Message] Sender: " + e.getSender() + "Key: " + e.key);
            if (isModIDBlacklisted(e.getSender())) continue;
            if (e.isStringMessage() && (e.key.equals("Discord-Message") || e.key.equals("sendMessage"))) {
                discord_instance.sendMessage(e.getStringValue());
            }
            //Compat with imc from another discord integration mod
            if (e.isNBTMessage() && e.key.equals("sendMessage")) {
                final CompoundNBT msg = e.getNBTValue();
                discord_instance.sendMessage(msg.getString("message"));
            }
        }
    } */
    private boolean isModIDBlacklisted(String sender) {
        return ArrayUtils.contains(Configuration.getArray(Configuration.INSTANCE.imcModIdBlacklist.get()), sender);
    }
    
    @SubscribeEvent
    public void playerLeave(PlayerEvent.PlayerLoggedOutEvent ev) {
        
        if (discord_instance != null && !ev.getPlayer().equals(lastTimeout)) discord_instance.sendMessage(Configuration.INSTANCE.msgPlayerLeave.get().replace("%player%", ev.getPlayer().getName().getUnformattedComponentText()));
        else if (discord_instance != null && ev.getPlayer().equals(lastTimeout)) {
            discord_instance.sendMessage(Configuration.INSTANCE.msgPlayerTimeout.get().replace("%player%", ev.getPlayer().getName().getUnformattedComponentText()));
            lastTimeout = null;
        }
        /*if (Loader.isModLoaded("votifier")) {
            MinecraftForge.EVENT_BUS.register(new VotifierEventHandler());
        }*/
    }
}
