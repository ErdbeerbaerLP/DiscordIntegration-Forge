package de.erdbeerbaerlp.dcintegration.forge;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import de.erdbeerbaerlp.dcintegration.common.Discord;
import de.erdbeerbaerlp.dcintegration.common.compat.DynmapListener;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.MCSubCommand;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.McCommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.*;
import de.erdbeerbaerlp.dcintegration.forge.api.ForgeDiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.forge.command.McCommandDiscord;
import de.erdbeerbaerlp.dcintegration.forge.util.ForgeMessageUtils;
import de.erdbeerbaerlp.dcintegration.forge.util.ForgeServerInterface;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkConstants;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.*;

@Mod(DiscordIntegration.MODID)
public class DiscordIntegration {
    /**
     * Modid
     */
    public static final String MODID = "dcintegration";
    /**
     * Contains timed-out player UUIDs, gets filled in MixinNetHandlerPlayServer
     */
    public static final ArrayList<UUID> timeouts = new ArrayList<>();
    private boolean stopped = false;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public DiscordIntegration() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        try {
            Discord.loadConfigs();
            if (FMLEnvironment.dist == Dist.CLIENT) {
                LOGGER.error("This mod cannot be used client-side");
            } else {
                MinecraftForge.EVENT_BUS.register(this);
                if (Configuration.instance().general.botToken.equals("INSERT BOT TOKEN HERE")) { //Prevent events when token not set or on client
                    LOGGER.error("Please check the config file and set an bot token");
                } else {
                    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverSetup);
                    //Create data directory if missing
                    if (!discordDataDir.exists()) discordDataDir.mkdir();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Config loading failed");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            LOGGER.error("Failed to read config file! Please check your config file!\nError description: " + e.getMessage());
            LOGGER.error("\nStacktrace: ");
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void serverSetup(FMLDedicatedServerSetupEvent ev) {
        Variables.discord_instance = new Discord(new ForgeServerInterface());
        try {
            //Wait a short time to allow JDA to get initialized
            LOGGER.info("Waiting for JDA to initialize to send starting message... (max 5 seconds before skipping)");
            for (int i = 0; i <= 5; i++) {
                if (discord_instance.getJDA() == null) Thread.sleep(1000);
                else break;
            }
            if (discord_instance.getJDA() != null) {
                Thread.sleep(2000); //Wait for it to cache the channels
                if (!Localization.instance().serverStarting.isEmpty()) {
                    CommandRegistry.registerDefaultCommandsFromConfig();
                    if (discord_instance.getChannel() != null)
                        Variables.startingMsg = discord_instance.sendMessageReturns(Localization.instance().serverStarting, discord_instance.getChannel(Configuration.instance().advanced.serverChannelID));
                }
            }
        } catch (InterruptedException | NullPointerException ignored) {
        }
    }

    @SubscribeEvent
    public void playerJoin(final PlayerEvent.PlayerLoggedInEvent ev) {
        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, ev.getEntity().getUUID()).hideFromDiscord) return;
            discord_instance.sendMessage(Localization.instance().playerJoin.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.getEntity())));

            // Fix link status (if user does not have role, give the role to the user, or vice versa)
            final Thread fixLinkStatus = new Thread(() -> {
                if (Configuration.instance().linking.linkedRoleID.equals("0")) return;
                final UUID uuid = ev.getEntity().getUUID();
                if (!PlayerLinkController.isPlayerLinked(uuid)) return;
                final Guild guild = discord_instance.getChannel().getGuild();
                final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
                if (PlayerLinkController.isPlayerLinked(uuid)) {
                    final Member member = guild.retrieveMemberById(PlayerLinkController.getDiscordFromPlayer(uuid)).complete();
                    if (!member.getRoles().contains(linkedRole))
                        guild.addRoleToMember(member, linkedRole).queue();
                }
            });
            fixLinkStatus.setDaemon(true);
            fixLinkStatus.start();
        }
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    @SubscribeEvent
    public void advancement(AdvancementEvent.AdvancementEarnEvent ev) {

        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, ev.getEntity().getUUID()).hideFromDiscord) return;
            if (ev.getEntity().getServer().getPlayerList().getPlayerAdvancements((ServerPlayer) ev.getEntity()).getOrStartProgress(ev.getAdvancement()).isDone())
                if (discord_instance != null && ev.getAdvancement() != null && ev.getAdvancement().getDisplay() != null && ev.getAdvancement().getDisplay().shouldAnnounceChat())
                    discord_instance.sendMessage(Localization.instance().advancementMessage.replace("%player%",
                                    ChatFormatting.stripFormatting(ForgeMessageUtils.formatPlayerName(ev.getEntity())))
                            .replace("%name%",
                                    ChatFormatting.stripFormatting(ev.getAdvancement()
                                            .getDisplay()
                                            .getTitle()
                                            .getString()))
                            .replace("%desc%",
                                    ChatFormatting.stripFormatting(ev.getAdvancement()
                                            .getDisplay()
                                            .getDescription()
                                            .getString()))
                            .replace("\\n", "\n"));

        }
    }

    @SubscribeEvent
    public void registerCommands(final RegisterCommandsEvent ev) {
        new McCommandDiscord(ev.getDispatcher());
    }

    @SubscribeEvent
    public void serverStarted(final ServerStartedEvent ev) {
        LOGGER.info("Started");
        Variables.started = new Date().getTime();
        if (discord_instance != null) {
            if (Variables.startingMsg != null) {
                Variables.startingMsg.thenAccept((a) -> a.editMessage(Localization.instance().serverStarted).queue());
            } else discord_instance.sendMessage(Localization.instance().serverStarted);
            discord_instance.startThreads();
            if (ModList.get().getModContainerById("dynmap").isPresent()) {
                new DynmapListener().register();
            }
        }
        UpdateChecker.runUpdateCheck("https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/1.19/update_checker.json");

        if (!DownloadSourceChecker.checkDownloadSource(new File(DiscordIntegration.class.getProtectionDomain().getCodeSource().getLocation().getPath().split("%")[0]))) {
            LOGGER.warn("You likely got this mod from a third party website.");
            LOGGER.warn("Some of such websites are distributing malware or old versions.");
            LOGGER.warn("Download this mod from an official source (https://www.curseforge.com/minecraft/mc-mods/dcintegration) to hide this message");
            LOGGER.warn("This warning can also be suppressed in the config file");
        }
    }

    @SubscribeEvent
    public void command(CommandEvent ev) {

        String command = ev.getParseResults().getReader().getString().replaceFirst(Pattern.quote("/"), "");
        if (!Configuration.instance().commandLog.channelID.equals("0")) {
            if (!ArrayUtils.contains(Configuration.instance().commandLog.ignoredCommands, command.split(" ")[0]))
                discord_instance.sendMessage(Configuration.instance().commandLog.message
                        .replace("%sender%", ev.getParseResults().getContext().getLastChild().getSource().getTextName())
                        .replace("%cmd%", command)
                        .replace("%cmd-no-args%", command.split(" ")[0]), discord_instance.getChannel(Configuration.instance().commandLog.channelID));
        }
        if (discord_instance != null) {
            boolean raw = false;

            final CommandSourceStack source = ev.getParseResults().getContext().getSource();
            final Entity sourceEntity = source.getEntity();
            if ((command.startsWith("me") && Configuration.instance().messages.sendOnMeCommand)) {
                String msg = command.replace("say ", "");
                if (command.startsWith("me")) {
                    raw = true;
                    msg = "*" + MessageUtils.escapeMarkdown(msg.replaceFirst("me ", "").trim()) + "*";
                }
                discord_instance.sendMessage(source.getTextName(), sourceEntity != null ? sourceEntity.getUUID().toString() : "0000000", new DiscordMessage(null, msg, !raw), discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID));
            }

            if(command.startsWith("discord ") || command.startsWith("dc ")){
                final String[] args = command.replace("discord ","").replace("dc ","").split(" ");
                for (MCSubCommand mcSubCommand : McCommandRegistry.getCommands()) {
                    if (args[0].equals(mcSubCommand.getName())) {
                        final String[] cmdArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                        switch (mcSubCommand.getType()) {
                            case CONSOLE_ONLY:
                                if (source.source instanceof MinecraftServer) {
                                    final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, null));
                                    try {
                                        source.source.sendSystemMessage(ComponentArgument.textComponent().parse(new StringReader(txt)));
                                    } catch (CommandSyntaxException ignored) {
                                    }
                                    break;
                                }else source.source.sendSystemMessage(net.minecraft.network.chat.Component.literal(Localization.instance().commands.consoleOnly));
                            case PLAYER_ONLY:
                                if ((source.source instanceof final Player p)) {
                                    if (!mcSubCommand.needsOP()) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, p.getUUID()));
                                        try {
                                            source.source.sendSystemMessage(ComponentArgument.textComponent().parse(new StringReader(txt)));
                                        } catch (CommandSyntaxException ignored) {
                                        }
                                    }else if(source.hasPermission(4)) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, p.getUUID()));
                                        try {
                                            source.source.sendSystemMessage(ComponentArgument.textComponent().parse(new StringReader(txt)));
                                        } catch (CommandSyntaxException ignored) {
                                        }
                                    }else{
                                        source.source.sendSystemMessage(net.minecraft.network.chat.Component.literal(Localization.instance().commands.noPermission));
                                    }
                                }else source.source.sendSystemMessage(net.minecraft.network.chat.Component.literal(Localization.instance().commands.ingameOnly));
                                break;
                            case BOTH:
                                if ((source.source instanceof final Player p)) {
                                    if (!mcSubCommand.needsOP()) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, p.getUUID()));
                                        try {
                                            source.source.sendSystemMessage(ComponentArgument.textComponent().parse(new StringReader(txt)));
                                        } catch (CommandSyntaxException ignored) {
                                        }
                                    }else if(source.hasPermission(4)) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, p.getUUID()));
                                        try {
                                            source.source.sendSystemMessage(ComponentArgument.textComponent().parse(new StringReader(txt)));
                                        } catch (CommandSyntaxException ignored) {
                                        }
                                    }else{
                                        source.source.sendSystemMessage(net.minecraft.network.chat.Component.literal(Localization.instance().commands.noPermission));
                                    }
                                } else {
                                    final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, null));
                                    try {
                                        source.source.sendSystemMessage(ComponentArgument.textComponent().parse(new StringReader(txt)));
                                    } catch (CommandSyntaxException ignored) {
                                    }
                                }
                        }
                    }
                    ev.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void serverStopping(ServerStoppedEvent ev) {
        if (discord_instance != null) {
            ev.getServer().executeBlocking(() -> {
                discord_instance.stopThreads();
                try {
                    if (!Configuration.instance().webhook.enable)
                        discord_instance.sendMessageReturns(
                                ev.getServer().isRunning() ? Localization.instance().serverCrash : Localization.instance().serverStopped,
                                discord_instance.getChannel(Configuration.instance().advanced.serverChannelID)
                        ).get();
                    else
                        discord_instance.sendMessage(ev.getServer().isRunning() ? Localization.instance().serverCrash : Localization.instance().serverStopped,
                                discord_instance.getChannel(Configuration.instance().advanced.serverChannelID));
                } catch (InterruptedException | ExecutionException ignored) {
                }
                discord_instance.kill();
                discord_instance = null;
                this.stopped = true;
                LOGGER.info("Shut-down successfully!");

            });
        }
    }

    @SubscribeEvent
    public void chat(ServerChatEvent ev) {
        if (discord_instance != null) {
            final ServerPlayer player = ev.getPlayer();
            if (player != null) {
                if (PlayerLinkController.getSettings(null, player.getUUID()).hideFromDiscord) return;
            }

            final net.minecraft.network.chat.Component msg = ev.getMessage();
            if (discord_instance.callEvent((e) -> {
                if (e instanceof ForgeDiscordEventHandler) {
                    return ((ForgeDiscordEventHandler) e).onMcChatMessage(ev);
                }
                return false;
            })) return;

            String text = MessageUtils.escapeMarkdown(ev.getMessage().getString().replace("@everyone", "[at]everyone").replace("@here", "[at]here"));
            final MessageEmbed embed = ForgeMessageUtils.genItemStackEmbedIfAvailable(msg);
            StandardGuildMessageChannel channel = discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID);
            if (channel == null) return;
            if (player != null) {
                discord_instance.sendMessage(ForgeMessageUtils.formatPlayerName(player), player.getUUID().toString(), new DiscordMessage(embed, text, true), channel);
            } else {
                discord_instance.sendMessage(ev.getUsername() != null ? ev.getUsername() : Configuration.instance().webhook.serverName, "0000000", new DiscordMessage(null, msg.getString()), discord_instance.getChannel(Configuration.instance().advanced.chatOutputChannelID));
            }
            final String json = net.minecraft.network.chat.Component.Serializer.toJson(msg);
            Component comp = GsonComponentSerializer.gson().deserialize(json);
            final String editedJson = GsonComponentSerializer.gson().serialize(MessageUtils.mentionsToNames(comp, channel.getGuild()));
            ev.setMessage(net.minecraft.network.chat.Component.Serializer.fromJson(editedJson));
        }

    }

    @SuppressWarnings("ConstantConditions")
    @SubscribeEvent
    public void death(LivingDeathEvent ev) {
        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, ev.getEntity().getUUID()).hideFromDiscord) return;
            if (ev.getEntity() instanceof Player || (ev.getEntity() instanceof TamableAnimal && ((TamableAnimal) ev.getEntity()).getOwner() instanceof Player && Configuration.instance().messages.sendDeathMessagesForTamedAnimals)) {
                final net.minecraft.network.chat.Component deathMessage = ev.getSource().getLocalizedDeathMessage(ev.getEntity());
                final MessageEmbed embed = ForgeMessageUtils.genItemStackEmbedIfAvailable(deathMessage);
                discord_instance.sendMessage(new DiscordMessage(embed, Localization.instance().playerDeath.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.getEntity())).replace("%msg%", ChatFormatting.stripFormatting(deathMessage.getString()).replace(ev.getEntity().getName().getString() + " ", "")).replace("@everyone", "[at]everyone").replace("@here", "[at]here")), discord_instance.getChannel(Configuration.instance().advanced.deathsChannelID));
            }
        }
    }

    @SubscribeEvent
    public void playerLeave(PlayerEvent.PlayerLoggedOutEvent ev) {
        if (stopped) return; //Try to fix player leave messages after stop!
        if (discord_instance != null) {
            if (PlayerLinkController.getSettings(null, ev.getEntity().getUUID()).hideFromDiscord) return;
            if (!timeouts.contains(ev.getEntity().getUUID()))
                discord_instance.sendMessage(Localization.instance().playerLeave.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.getEntity())));
            else if (timeouts.contains(ev.getEntity().getUUID())) {
                discord_instance.sendMessage(Localization.instance().playerTimeout.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.getEntity())));
                timeouts.remove(ev.getEntity().getUUID());
            }
        }
    }
}
