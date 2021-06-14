package de.erdbeerbaerlp.dcintegration.forge;

import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import dcshadow.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import de.erdbeerbaerlp.dcintegration.common.Discord;
import de.erdbeerbaerlp.dcintegration.common.compat.DynmapListener;
import de.erdbeerbaerlp.dcintegration.common.discordCommands.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.UpdateChecker;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;
import de.erdbeerbaerlp.dcintegration.forge.api.ForgeDiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.forge.command.McCommandDiscord;
import de.erdbeerbaerlp.dcintegration.forge.mixin.MixinNetHandlerPlayServer;
import de.erdbeerbaerlp.dcintegration.forge.util.ForgeMessageUtils;
import de.erdbeerbaerlp.dcintegration.forge.util.ForgeServerInterface;
import net.dv8tion.jda.api.entities.*;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discordDataDir;
import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;

@Mod(modid = DiscordIntegration.MODID, version = "2.2.0", name = "Discord Integration", serverSideOnly = true, acceptableRemoteVersions = "*")

public class DiscordIntegration {
    /**
     * Modid
     */
    public static final String MODID = "dcintegration";
    /**
     * Contains timed-out player UUIDs, gets filled in {@link MixinNetHandlerPlayServer}
     */
    public static final ArrayList<UUID> timeouts = new ArrayList<>();
    private boolean stopped = false;

    public DiscordIntegration() {
    }

    @Mod.EventHandler
    public void modConstruction(FMLConstructionEvent ev) {
        Configuration.instance().loadConfig();
        if (!Configuration.instance().general.botToken.equals("INSERT BOT TOKEN HERE")) { //Prevent events when token not set
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            System.err.println("Please check the config file and set an bot token");
        }

        //  ==  Migrate some files from 1.x.x to 2.x.x  ==

        //LinkedPlayers JSON file
        final File linkedOld = new File("./linkedPlayers.json");
        final File linkedNew = new File(discordDataDir, "LinkedPlayers.json");

        //Player Ignores
        final File ignoreOld = new File("./players_ignoring_discord_v2");
        final File ignoreNew = new File(discordDataDir, ".PlayerIgnores");

        //Create data directory if missing
        if (!discordDataDir.exists()) discordDataDir.mkdir();

        //Move Files
        if (linkedOld.exists() && !linkedNew.exists()) {
            linkedOld.renameTo(linkedNew);
        }
        if (ignoreOld.exists() && !ignoreNew.exists()) {
            ignoreOld.renameTo(ignoreNew);
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent ev) {


        CommandRegistry.registerDefaultCommandsFromConfig();
        Variables.discord_instance = new Discord(new ForgeServerInterface());
        try {
            //Wait a short time to allow JDA to get initiaized
            System.out.println("Waiting for JDA to initialize to send starting message... (max 5 seconds before skipping)");
            for (int i = 0; i <= 5; i++) {
                if (discord_instance.getJDA() == null) Thread.sleep(1000);
                else break;
            }
            if (discord_instance.getJDA() != null && !Configuration.instance().localization.serverStarting.isEmpty()) {
                Thread.sleep(2000); //Wait for it to cache the channels
                if (discord_instance.getChannel() != null)
                    Variables.startingMsg = discord_instance.sendMessageReturns(Configuration.instance().localization.serverStarting, discord_instance.getChannel(Configuration.instance().advanced.serverChannelID));
            }
        } catch (InterruptedException | NullPointerException ignored) {
        }
        if (Loader.isModLoaded("votifier")) {
            MinecraftForge.EVENT_BUS.register(new VotifierEventHandler());
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent ev) {
        ev.registerServerCommand(new McCommandDiscord());
    }


    @Mod.EventHandler
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

    @SubscribeEvent
    public void playerJoin(final net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent ev) {
        if (PlayerLinkController.getSettings(null, ev.player.getUniqueID()).hideFromDiscord) return;
        if (discord_instance != null) {
            discord_instance.sendMessage(Configuration.instance().localization.playerJoin.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.player)));

            // Fix link status (if user does not have role, give the role to the user, or vice versa)
            final Thread fixLinkStatus = new Thread(() -> {
                if (Configuration.instance().linking.linkedRoleID.equals("0")) return;
                final UUID uuid = ev.player.getUniqueID();
                if (!PlayerLinkController.isPlayerLinked(uuid)) return;
                final Guild guild = discord_instance.getChannel().getGuild();
                final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
                if (PlayerLinkController.isPlayerLinked(uuid)) {
                    final Member member = guild.getMemberById(PlayerLinkController.getDiscordFromPlayer(uuid));
                    if (!member.getRoles().contains(linkedRole))
                        guild.addRoleToMember(member, linkedRole).queue();
                }
            });
            fixLinkStatus.setDaemon(true);
            fixLinkStatus.start();
        }
    }

    @SubscribeEvent
    public void advancement(AdvancementEvent ev) {
        if (PlayerLinkController.getSettings(null, ev.getEntityPlayer().getUniqueID()).hideFromDiscord) return;
        if (discord_instance != null && ev.getAdvancement() != null && ev.getAdvancement().getDisplay() != null && ev.getAdvancement().getDisplay().shouldAnnounceToChat())
            discord_instance.sendMessage(Configuration.instance().localization.advancementMessage.replace("%player%",
                    TextFormatting.getTextWithoutFormattingCodes(ForgeMessageUtils.formatPlayerName(ev.getEntityPlayer())))
                    .replace("%name%",
                            TextFormatting.getTextWithoutFormattingCodes(ev.getAdvancement()
                                    .getDisplay()
                                    .getTitle()
                                    .getUnformattedText()))
                    .replace("%desc%",
                            TextFormatting.getTextWithoutFormattingCodes(ev.getAdvancement()
                                    .getDisplay()
                                    .getDescription()
                                    .getUnformattedText()))
                    .replace("\\n", "\n"));


    }

    @Mod.EventHandler
    public void serverStarted(final FMLServerStartedEvent ev) {
        System.out.println("Started");
        Variables.started = new Date().getTime();
        if (discord_instance != null)
            if (Variables.startingMsg != null) {
                Variables.startingMsg.thenAccept((a) -> a.editMessage(Configuration.instance().localization.serverStarted).queue());
            } else discord_instance.sendMessage(Configuration.instance().localization.serverStarted);
        if (discord_instance != null) {
            discord_instance.startThreads();
        }
        UpdateChecker.runUpdateCheck();
        if (Loader.isModLoaded("dynmap")) {
            new DynmapListener().register();
        }
    }

    @SubscribeEvent
    public void command(CommandEvent ev) {
        String command = ev.getCommand().getName() + " ";
        for (String s : ev.getParameters()) {
            command += s + " ";
        }
        if (!Configuration.instance().commandLog.channelID.equals("0")) {
            if (!ArrayUtils.contains(Configuration.instance().commandLog.ignoredCommands, command.split(" ")[0]))
                discord_instance.sendMessage(Configuration.instance().commandLog.message
                        .replace("%sender%", ev.getSender().getName())
                        .replace("%cmd%", command)
                        .replace("%cmd-no-args%", command.split(" ")[0]), discord_instance.getChannel(Configuration.instance().commandLog.channelID));
        }
        if (discord_instance != null) {
            boolean raw = false;

            if (((command.startsWith("say")) && Configuration.instance().messages.sendOnSayCommand) || (command.startsWith("me") && Configuration.instance().messages.sendOnMeCommand)) {
                String msg = command.replace("say ", "");
                if (command.startsWith("say"))
                    msg = msg.replaceFirst("say ", "");
                if (command.startsWith("me")) {
                    raw = true;
                    msg = "*" + MessageUtils.escapeMarkdown(msg.replaceFirst("me ", "").trim()) + "*";
                }
                if (ev.getSender() instanceof DedicatedServer) discord_instance.sendMessage(msg);
                else if (ev.getSender() instanceof EntityPlayer || ev.getSender() instanceof FakePlayer)
                    discord_instance.sendMessage(ev.getCommand().getName(), ev.getSender().getCommandSenderEntity().getUniqueID().toString(), new DiscordMessage(null, msg, !raw), discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID));
            }
        }
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent ev) {
        if (discord_instance != null) {
            discord_instance.sendMessage(Configuration.instance().localization.serverStopped);
            discord_instance.stopThreads();
        }
        this.stopped = true;
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent ev) {
        FMLCommonHandler.instance().getMinecraftServerInstance().callFromMainThread(Executors.callable(this::stopDiscord)); //Attempt to force send the messages before server finally closes
    }
    public void stopDiscord(){
        if (discord_instance != null && !stopped && discord_instance.getJDA() != null) {
            discord_instance.stopThreads();
            try {
                if (Configuration.instance().webhook.enable)
                    discord_instance.sendMessageReturns(Configuration.instance().localization.serverCrash, discord_instance.getChannel(Configuration.instance().advanced.serverChannelID)).get();
                else
                    discord_instance.sendMessage(Configuration.instance().localization.serverCrash, discord_instance.getChannel(Configuration.instance().advanced.serverChannelID));
            } catch (InterruptedException | ExecutionException ignored) {
            }
            discord_instance.kill();
        }
    }

    private boolean isModIDBlacklisted(String sender) {
        return ArrayUtils.contains(Configuration.instance().forgeSpecific.IMC_modIdBlacklist, sender);
    }

    @SubscribeEvent
    public void chat(ServerChatEvent ev) {
        if (PlayerLinkController.getSettings(null, ev.getPlayer().getUniqueID()).hideFromDiscord) return;
        final ITextComponent msg = ev.getComponent();
        if (discord_instance.callEvent((e) -> {
            if (e instanceof ForgeDiscordEventHandler) {
                return ((ForgeDiscordEventHandler) e).onMcChatMessage(ev);
            }
            return false;
        })) return;

        String text = MessageUtils.escapeMarkdown(ev.getMessage().replace("@everyone", "[at]everyone").replace("@here", "[at]here"));
        final MessageEmbed embed = ForgeMessageUtils.genItemStackEmbedIfAvailable(msg);
        if (discord_instance != null) {
            TextChannel channel = discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID);
            if (channel == null) return;
            discord_instance.sendMessage(ForgeMessageUtils.formatPlayerName(ev.getPlayer()), ev.getPlayer().getUniqueID().toString(), new DiscordMessage(embed, text, true), channel);
            final String componentText = msg.getUnformattedText();
            Component comp = LegacyComponentSerializer.legacySection().deserialize(componentText);
            final String editedJson = GsonComponentSerializer.gson().serialize(MessageUtils.mentionsToNames(comp, channel.getGuild()));
            ev.setComponent(ITextComponent.Serializer.jsonToComponent(editedJson));
        }

    }

    @SubscribeEvent
    public void death(LivingDeathEvent ev) {
        if (PlayerLinkController.getSettings(null, ev.getEntity().getUniqueID()).hideFromDiscord) return;
        if (ev.getEntity() instanceof EntityPlayer || (ev.getEntity() instanceof EntityTameable && ((EntityTameable) ev.getEntity()).getOwner() instanceof EntityPlayer && Configuration.instance().messages.sendDeathMessagesForTamedAnimals)) {
            if (discord_instance != null) {
                final ITextComponent deathMessage = ev.getSource().getDeathMessage(ev.getEntityLiving());
                final MessageEmbed embed = ForgeMessageUtils.genItemStackEmbedIfAvailable(deathMessage);
                discord_instance.sendMessage(new DiscordMessage(embed, Configuration.instance().localization.playerDeath.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.getEntity())).replace("%msg%", TextFormatting.getTextWithoutFormattingCodes(deathMessage.getUnformattedText()).replace(ev.getEntity().getName() + " ", ""))), discord_instance.getChannel(Configuration.instance().advanced.deathsChannelID));
            }
        }
    }

    @SubscribeEvent
    public void playerLeave(PlayerEvent.PlayerLoggedOutEvent ev) {
        if (stopped) return; //Try to fix player leave messages after stop!
        if (PlayerLinkController.getSettings(null, ev.player.getUniqueID()).hideFromDiscord) return;
        if (discord_instance != null && !timeouts.contains(ev.player.getUniqueID()))
            discord_instance.sendMessage(Configuration.instance().localization.playerLeave.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.player)));
        else if (discord_instance != null && timeouts.contains(ev.player.getUniqueID())) {
            discord_instance.sendMessage(Configuration.instance().localization.playerTimeout.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.player)));
            timeouts.remove(ev.player.getUniqueID());
        }
    }
}
