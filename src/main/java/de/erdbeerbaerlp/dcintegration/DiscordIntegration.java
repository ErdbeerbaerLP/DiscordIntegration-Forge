package de.erdbeerbaerlp.dcintegration;


import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.erdbeerbaerlp.dcintegration.commands.*;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.requests.RequestFuture;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.server.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("ConstantConditions")
@Mod(DiscordIntegration.MODID)
public class DiscordIntegration {
    /**
     * Mod name
     */
    public static final String NAME = "Discord Integration";
    /**
     * Mod version
     */
    public static final String VERSION = "1.0.0";
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
    private RequestFuture<Message> startingMsg;
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
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverAboutToStart);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverStarting);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverStarted);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverStopping);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverStopped);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public static void playerJoin(final PlayerLoggedInEvent ev) {
        if (discord_instance != null) discord_instance.sendMessage(
                Configuration.INSTANCE.msgPlayerJoin.get()
                        .replace("%player%", ev.getPlayer().getName().getUnformattedComponentText())
        );
    }

    @SubscribeEvent
    public static void onModConfigEvent(final ModConfig.ModConfigEvent event) {
        final ModConfig config = event.getConfig();
        // Rebake the configs when they change
        if (config.getSpec() == Configuration.cfgSpec) {
            cfg = config;
        }
    }

    @SubscribeEvent
    public static void command(CommandEvent ev) {
        if (discord_instance != null)
            try {
                if (ev.getParseResults().getContext().getRootNode().getName().equals("say")) {

                    String msg = MessageArgument.getMessage(ev.getParseResults().getContext().build("say"), "message").getUnformattedComponentText();

                    System.out.println(ev.getParseResults().getContext().getSource().getClass().getCanonicalName());
                    //				if(ev.getParseResults().getContext() instanceof DedicatedServer)
                    //					discord_instance.sendMessage(msg);
                    //				else if(ev.getSender().getCommandSenderEntity() instanceof EntityPlayer)
                    //					discord_instance.sendMessage(ev.getSender().getName(), ev.getSender().getCommandSenderEntity().getUniqueID().toString(), msg);
                }
            } catch (CommandSyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    @SubscribeEvent
    public static void chat(ServerChatEvent ev) {
        if (discord_instance != null) discord_instance.sendMessage(ev.getPlayer(), ev.getMessage());
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent ev) {
        if (ev.getEntity() instanceof PlayerEntity) {
            if (discord_instance != null) discord_instance.sendMessage(
                    Configuration.INSTANCE.msgPlayerDeath.get()
                            .replace("%player%", ev.getEntity().getName().getUnformattedComponentText())
                            .replace("%msg%", ev.getSource().getDeathMessage(ev.getEntityLiving()).getUnformattedComponentText().replace(ev.getEntity().getName() + " ", ""))
            );
        }
    }

    @SubscribeEvent
    public static void advancement(AdvancementEvent ev) {
        if (discord_instance != null && ev.getAdvancement().getDisplay() != null && ev.getAdvancement().getDisplay().shouldAnnounceToChat())
            discord_instance.sendMessage(
                    Configuration.INSTANCE.msgAdvancement.get()
                            .replace("%player%", ev.getEntityPlayer().getName().getUnformattedComponentText())
                            .replace("%name%", ev.getAdvancement().getDisplay().getTitle().getUnformattedComponentText())
                            .replace("%desc%", ev.getAdvancement().getDisplay().getDescription().getUnformattedComponentText())
                            .replace("\\n", "\n")
            );
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

    public void preInit(final FMLDedicatedServerSetupEvent ev) {
        System.out.println("Loading mod");
        try {
            discord_instance = new Discord();
            discord_instance.registerCommand(new CommandHelp());
            discord_instance.registerCommand(new CommandList());
            discord_instance.registerCommand(new CommandKill());
            discord_instance.registerCommand(new CommandStop());
            discord_instance.registerCommand(new CommandKick());
            discord_instance.registerCommand(new CommandUptime());
        } catch (Exception e) {
            System.err.println("Failed to login: " + e.getMessage());
            discord_instance = null;
        }
        if (discord_instance != null && !Configuration.INSTANCE.enableWebhook.get())
            this.startingMsg = discord_instance.sendMessageReturns("Server Starting...");
        if (discord_instance != null && Configuration.INSTANCE.botModifyDescription.get())
            discord_instance.getChannelManager().setTopic(Configuration.INSTANCE.descriptionStarting.get()).complete();
    }

    public void serverAboutToStart(final FMLServerAboutToStartEvent ev) {

    }

    public void serverStarting(final FMLServerStartingEvent ev) {
        new McCommandDiscord(ev.getCommandDispatcher());
    }

    public void serverStarted(final FMLServerStartedEvent ev) {
        started = new Date().getTime();
        if (discord_instance != null) if (startingMsg != null) try {
            this.startingMsg.get().editMessage(Configuration.INSTANCE.msgServerStarted.get()).queue();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        else discord_instance.sendMessage(Configuration.INSTANCE.msgServerStarted.get());
        if (discord_instance != null) {
            if (Configuration.INSTANCE.botModifyDescription.get()) discord_instance.updateChannelDesc.start();
            //			if(Loader.isModLoaded("ftbutilities")) {
            //				if(FTBUtilitiesConfig.auto_shutdown.enabled) discord_instance.ftbUtilitiesShutdownDetectThread.start();
            //				if(FTBUtilitiesConfig.afk.enabled) discord_instance.ftbUtilitiesAFKDetectThread.start();
            //			}
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (discord_instance != null) {
                if (!stopped) {
                    if (!discord_instance.isKilled) {
                        discord_instance.stopThreads();
                        if (Configuration.INSTANCE.botModifyDescription.get())
                            discord_instance.getChannelManager().setTopic(Configuration.INSTANCE.msgServerCrash.get()).complete();
                        discord_instance.sendMessage(Configuration.INSTANCE.msgServerCrash.get());
                    }
                }
                discord_instance.kill();
            }
        }));

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

    public void serverStopping(final FMLServerStoppingEvent ev) {
        if (discord_instance != null) {
            discord_instance.stopThreads();
            if (Configuration.INSTANCE.enableWebhook.get()) {
                final WebhookMessageBuilder b = new WebhookMessageBuilder();
                b.setContent(Configuration.INSTANCE.msgServerStopped.get());
                b.setUsername(Configuration.INSTANCE.serverName.get());
                b.setAvatarUrl(Configuration.INSTANCE.serverAvatar.get());
                final WebhookClient cli = discord_instance.getWebhook().newClient().build();
                cli.send(b.build());
                cli.close();
            } else discord_instance.getChannel().sendMessage(Configuration.INSTANCE.msgServerStopped.get()).complete();
            discord_instance.getChannelManager().setTopic(Configuration.INSTANCE.descriptionOffline.get()).complete();
        }
        stopped = true;
    }

    public void serverStopped(final FMLServerStoppedEvent ev) {
        if (discord_instance != null) {
            if (!stopped) {
                if (!discord_instance.isKilled) {
                    discord_instance.stopThreads();
                    if (Configuration.INSTANCE.botModifyDescription.get())
                        discord_instance.getChannelManager().setTopic(Configuration.INSTANCE.msgServerCrash.get()).complete();
                    discord_instance.sendMessage(Configuration.INSTANCE.msgServerCrash.get());
                }
            }
            discord_instance.kill();
        }
    }

    @SubscribeEvent
    public void playerLeave(PlayerLoggedOutEvent ev) {

        if (discord_instance != null && !ev.getPlayer().equals(lastTimeout))
            discord_instance.sendMessage(
                    Configuration.INSTANCE.msgPlayerLeave.get()
                            .replace("%player%", ev.getPlayer().getName().getUnformattedComponentText())
            );
        else if (discord_instance != null && ev.getPlayer().equals(lastTimeout)) {
            discord_instance.sendMessage(
                    Configuration.INSTANCE.msgPlayerTimeout.get()
                            .replace("%player%", ev.getPlayer().getName().getUnformattedComponentText())
            );
            lastTimeout = null;
        }
    }
}
