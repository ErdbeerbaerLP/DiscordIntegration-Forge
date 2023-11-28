package de.erdbeerbaerlp.dcintegration.forge;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dcshadow.dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.WorkThread;
import de.erdbeerbaerlp.dcintegration.common.compat.DynmapListener;
import de.erdbeerbaerlp.dcintegration.common.compat.LuckpermsUtils;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.MCSubCommand;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.McCommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.*;
import de.erdbeerbaerlp.dcintegration.forge.api.ForgeDiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.forge.command.McCommandDiscord;
import de.erdbeerbaerlp.dcintegration.forge.util.ForgeMessageUtils;
import de.erdbeerbaerlp.dcintegration.forge.util.ForgeServerInterface;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.*;

@Mod(DiscordIntegrationMod.MODID)
public class DiscordIntegrationMod {
    /**
     * Modid
     */
    public static final String MODID = "dcintegration";
    /**
     * Contains timed-out player UUIDs, gets filled in MixinNetHandlerPlayServer
     */
    public static final ArrayList<UUID> timeouts = new ArrayList<>();
    private boolean stopped = false;

    public DiscordIntegrationMod() {
        LOGGER.info("Version is " + VERSION);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        try {
            //Create data directory if missing
            if (!discordDataDir.exists()) discordDataDir.mkdir();
            DiscordIntegration.loadConfigs();
            if (FMLEnvironment.dist == Dist.CLIENT) {
                LOGGER.error("This mod cannot be used client-side");
            } else {
                if (Configuration.instance().general.botToken.equals("INSERT BOT TOKEN HERE")) { //Prevent events when token not set or on client
                    LOGGER.error("Please check the config file and set an bot token");
                } else {
                    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverSetup);
                    MinecraftForge.EVENT_BUS.register(this);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Config loading failed");
            if (!discordDataDir.exists())
                LOGGER.error("Please create the folder " + discordDataDir.getAbsolutePath() + " manually");
            LOGGER.error(e.getMessage());
            LOGGER.error(e.getCause());
        } catch (IllegalStateException e) {
            LOGGER.error("Failed to read config file! Please check your config file!\nError description: " + e.getMessage());
            LOGGER.error("\nStacktrace: ");
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void serverSetup(FMLDedicatedServerSetupEvent ev) {
        INSTANCE = new DiscordIntegration(new ForgeServerInterface());
        try {
            //Wait a short time to allow JDA to get initialized
            LOGGER.info("Waiting for JDA to initialize to send starting message... (max 5 seconds before skipping)");
            for (int i = 0; i <= 5; i++) {
                if (INSTANCE.getJDA() == null) Thread.sleep(1000);
                else break;
            }
            if (DiscordIntegration.INSTANCE.getJDA() != null) {
                Thread.sleep(2000); //Wait for it to cache the channels
                CommandRegistry.registerDefaultCommands();
                if (!Localization.instance().serverStarting.isBlank())
                    if (DiscordIntegration.INSTANCE.getChannel() != null) {
                        final MessageCreateData m;
                        if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.startMessages.asEmbed)
                            m = new MessageCreateBuilder().setEmbeds(Configuration.instance().embedMode.startMessages.toEmbed().setDescription(Localization.instance().serverStarting).build()).build();
                        else
                            m = new MessageCreateBuilder().addContent(Localization.instance().serverStarting).build();
                        DiscordIntegration.startingMsg = DiscordIntegration.INSTANCE.sendMessageReturns(m, DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    }
            }
        } catch (InterruptedException | NullPointerException ignored) {
        }
    }

    public static final HashMap<String, PermissionNode<Boolean>> nodes = new HashMap();

    @SubscribeEvent
    public void addPermissions(final PermissionGatherEvent.Nodes ev) {
        for (MinecraftPermission p : MinecraftPermission.values()) {
            nodes.put(p.getAsString(), new PermissionNode<>("dcintegration", p.getAsString().replace("dcintegration.", ""), PermissionTypes.BOOLEAN, (player, playerUUID, context) -> p.getDefaultValue()));
        }
        ev.addNodes(nodes.values().toArray(new PermissionNode[0]));
    }

    @SubscribeEvent
    public void playerJoin(final PlayerEvent.PlayerLoggedInEvent ev) {
        if (INSTANCE != null) {
            if (LinkManager.isPlayerLinked(ev.getEntity().getUUID()) && LinkManager.getLink(null, ev.getEntity().getUUID()).settings.hideFromDiscord)
                return;
            LinkManager.checkGlobalAPI(ev.getEntity().getUUID());
            if (!Localization.instance().playerJoin.isBlank()) {
                final Player p = ev.getEntity();
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerJoinMessage.asEmbed) {
                    final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", p.getUUID().toString()).replace("%uuid_dashless%", p.getUUID().toString().replace("-", "")).replace("%name%", p.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.playerJoinMessage.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbedJson(Configuration.instance().embedMode.playerJoinMessage.customJSON
                                .replace("%uuid%", p.getUUID().toString())
                                .replace("%uuid_dashless%", p.getUUID().toString().replace("-", ""))
                                .replace("%name%", ForgeMessageUtils.formatPlayerName(p))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(p.getUUID()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbed();
                        b.setAuthor(ForgeMessageUtils.formatPlayerName(p), null, avatarURL)
                                .setDescription(Localization.instance().playerJoin.replace("%player%", ForgeMessageUtils.formatPlayerName(p)));
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerJoin.replace("%player%", ForgeMessageUtils.formatPlayerName(p)));
            }
            // Fix link status (if user does not have role, give the role to the user, or vice versa)
            WorkThread.executeJob(() -> {
                if (Configuration.instance().linking.linkedRoleID.equals("0")) return;
                final UUID uuid = ev.getEntity().getUUID();
                if (!LinkManager.isPlayerLinked(uuid)) return;
                final Guild guild = INSTANCE.getChannel().getGuild();
                final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);
                if (LinkManager.isPlayerLinked(uuid)) {
                    final Member member = DiscordIntegration.INSTANCE.getMemberById(LinkManager.getLink(null, uuid).discordID);
                    if (!member.getRoles().contains(linkedRole))
                        guild.addRoleToMember(member, linkedRole).queue();
                }
            });
        }
    }

    @SubscribeEvent
    public void advancement(AdvancementEvent.AdvancementEarnEvent ev) {
        if (Localization.instance().advancementMessage.isBlank()) return;
        if (LinkManager.isPlayerLinked(ev.getEntity().getUUID()) && LinkManager.getLink(null, ev.getEntity().getUUID()).settings.hideFromDiscord)
            return;
        if (ev.getEntity().getServer().getPlayerList().getPlayerAdvancements((ServerPlayer) ev.getEntity()).getOrStartProgress(ev.getAdvancement()).isDone())
            if (INSTANCE != null && ev.getAdvancement() != null && ev.getAdvancement().getDisplay() != null && ev.getAdvancement().getDisplay().shouldAnnounceChat())
                if (!Localization.instance().advancementMessage.isBlank()) {
                    if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.advancementMessage.asEmbed) {
                        final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", ev.getEntity().getUUID().toString()).replace("%uuid_dashless%", ev.getEntity().getUUID().toString().replace("-", "")).replace("%name%", ev.getEntity().getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                        if (!Configuration.instance().embedMode.advancementMessage.customJSON.isBlank()) {
                            final EmbedBuilder b = Configuration.instance().embedMode.advancementMessage.toEmbedJson(Configuration.instance().embedMode.advancementMessage.customJSON
                                    .replace("%uuid%", ev.getEntity().getUUID().toString())
                                    .replace("%uuid_dashless%", ev.getEntity().getUUID().toString().replace("-", ""))
                                    .replace("%name%", ForgeMessageUtils.formatPlayerName(ev.getEntity()))
                                    .replace("%randomUUID%", UUID.randomUUID().toString())
                                    .replace("%avatarURL%", avatarURL)
                                    .replace("%advName%", ChatFormatting.stripFormatting(ev.getAdvancement().getDisplay().getTitle().getString()))
                                    .replace("%advDesc%", ChatFormatting.stripFormatting(ev.getAdvancement().getDisplay().getDescription().getString()))
                                    .replace("%avatarURL%", avatarURL)
                                    .replace("%playerColor%", "" + TextColors.generateFromUUID(ev.getEntity().getUUID()).getRGB())
                            );
                            INSTANCE.sendMessage(new DiscordMessage(b.build()));
                        } else {
                            EmbedBuilder b = Configuration.instance().embedMode.advancementMessage.toEmbed();
                            b = b.setAuthor(ForgeMessageUtils.formatPlayerName(ev.getEntity()), null, avatarURL)
                                    .setDescription(Localization.instance().advancementMessage.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.getEntity())).replace("%advName%",
                                                    ChatFormatting.stripFormatting(ev.getAdvancement()
                                                            .getDisplay()
                                                            .getTitle()
                                                            .getString()))
                                            .replace("%advDesc%",
                                                    ChatFormatting.stripFormatting(ev.getAdvancement()
                                                            .getDisplay()
                                                            .getDescription()
                                                            .getString()))
                                            .replace("\\n", "\n"));
                            INSTANCE.sendMessage(new DiscordMessage(b.build()));
                        }
                    } else INSTANCE.sendMessage(Localization.instance().advancementMessage.replace("%player%",
                                    ChatFormatting.stripFormatting(ForgeMessageUtils.formatPlayerName(ev.getEntity())))
                            .replace("%advName%",
                                    ChatFormatting.stripFormatting(ev.getAdvancement()
                                            .getDisplay()
                                            .getTitle()
                                            .getString()))
                            .replace("%advDesc%",
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
        started = new Date().getTime();
        if (INSTANCE != null) {
            if (DiscordIntegration.startingMsg != null) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.startMessages.asEmbed) {
                    if (!Configuration.instance().embedMode.startMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.startMessages.toEmbedJson(Configuration.instance().embedMode.startMessages.customJSON);
                        DiscordIntegration.startingMsg.thenAccept((a) -> a.editMessageEmbeds(b.build()).queue());
                    } else
                        DiscordIntegration.startingMsg.thenAccept((a) -> a.editMessageEmbeds(Configuration.instance().embedMode.startMessages.toEmbed().setDescription(Localization.instance().serverStarted).build()).queue());
                } else
                    DiscordIntegration.startingMsg.thenAccept((a) -> a.editMessage(Localization.instance().serverStarted).queue());
            } else {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.startMessages.asEmbed) {
                    if (!Configuration.instance().embedMode.startMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.startMessages.toEmbedJson(Configuration.instance().embedMode.startMessages.customJSON);
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(Configuration.instance().embedMode.startMessages.toEmbed().setDescription(Localization.instance().serverStarted).build()));
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().serverStarted);
            }
            INSTANCE.startThreads();
        }
        UpdateChecker.runUpdateCheck("https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/1.20.1/update_checker.json");
        if (ModList.get().getModContainerById("dynmap").isPresent()) {
            new DynmapListener().register();
        }


        if (!DownloadSourceChecker.checkDownloadSource(new File(DiscordIntegrationMod.class.getProtectionDomain().getCodeSource().getLocation().getPath().split("%")[0]))) {
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
                INSTANCE.sendMessage(Configuration.instance().commandLog.message
                        .replace("%sender%", ev.getParseResults().getContext().getLastChild().getSource().getTextName())
                        .replace("%cmd%", command)
                        .replace("%cmd-no-args%", command.split(" ")[0]), INSTANCE.getChannel(Configuration.instance().commandLog.channelID));
        }
        if (INSTANCE != null) {
            final CommandSourceStack source = ev.getParseResults().getContext().getSource();
            final Entity sourceEntity = source.getEntity();
            boolean raw = false;

            if (((command.startsWith("say")) && Configuration.instance().messages.sendOnSayCommand) || (command.startsWith("me") && Configuration.instance().messages.sendOnMeCommand)) {
                String msg = command.replace("say ", "");
                if (command.startsWith("say"))
                    msg = msg.replaceFirst("say ", "");
                if (command.startsWith("me")) {
                    raw = true;
                    msg = "*" + MessageUtils.escapeMarkdown(msg.replaceFirst("me ", "").trim()) + "*";
                }
                INSTANCE.sendMessage(source.getTextName(), sourceEntity != null ? sourceEntity.getUUID().toString() : "0000000", new DiscordMessage(null, msg, !raw), INSTANCE.getChannel(Configuration.instance().advanced.chatOutputChannelID));
            }

            if (command.startsWith("tellraw ") && !Configuration.instance().messages.tellrawSelector.isBlank()) {
                final String[] args = command.replace("tellraw ", "").replace("dc ", "").split(" ");
                if (args[0].equals(Configuration.instance().messages.tellrawSelector)) {
                    INSTANCE.sendMessage(DiscordSerializer.INSTANCE.serialize(GsonComponentSerializer.gson().deserialize(command.replace("tellraw " + args[0], ""))));
                }
            }
            if (command.startsWith("discord ") || command.startsWith("dc ")) {
                final String[] args = command.replace("discord ", "").replace("dc ", "").split(" ");
                for (MCSubCommand mcSubCommand : McCommandRegistry.getCommands()) {
                    if (args[0].equals(mcSubCommand.getName())) {
                        final String[] cmdArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                        switch (mcSubCommand.getType()) {
                            case CONSOLE_ONLY:
                                try {
                                    source.getPlayerOrException();
                                    source.sendFailure(net.minecraft.network.chat.Component.nullToEmpty(Localization.instance().commands.consoleOnly));
                                } catch (CommandSyntaxException e) {
                                    final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, null));

                                    source.sendSuccess(() -> {
                                        try {
                                            return ComponentArgument.textComponent().parse(new StringReader(txt));
                                        } catch (CommandSyntaxException ignored) {
                                            return null;
                                        }
                                    }, false);

                                }
                                break;
                            case PLAYER_ONLY:
                                try {
                                    final ServerPlayer player = source.getPlayerOrException();
                                    if (!mcSubCommand.needsOP() && !(ModList.get().getModContainerById("dynmap").isPresent() && LuckpermsUtils.uuidHasPermission(MinecraftPermission.RUN_DISCORD_COMMAND.getAsString(), player.getUUID()))) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));


                                        source.sendSuccess(() -> {
                                            try {
                                                return ComponentArgument.textComponent().parse(new StringReader(txt));
                                            } catch (CommandSyntaxException ignored) {
                                                return null;
                                            }
                                        }, false);
                                    } else if (source.hasPermission(4)) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));

                                        source.sendSuccess(() -> {
                                            try {
                                                return ComponentArgument.textComponent().parse(new StringReader(txt));
                                            } catch (CommandSyntaxException ignored) {
                                                return null;
                                            }
                                        }, false);
                                    } else if (ModList.get().getModContainerById("luckperms").isPresent() && (LuckpermsUtils.uuidHasPermission(MinecraftPermission.RUN_DISCORD_COMMAND_ADMIN.getAsString(), player.getUUID()) || LuckpermsUtils.uuidHasPermission(MinecraftPermission.ADMIN.getAsString(), player.getUUID()))) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));
                                        source.sendSuccess(() -> {
                                            try {
                                                return ComponentArgument.textComponent().parse(new StringReader(txt));
                                            } catch (CommandSyntaxException ignored) {
                                                return null;
                                            }
                                        }, false);
                                    } else {
                                        source.sendFailure(net.minecraft.network.chat.Component.nullToEmpty(Localization.instance().commands.noPermission));
                                    }
                                } catch (CommandSyntaxException e) {
                                    source.sendFailure(net.minecraft.network.chat.Component.nullToEmpty(Localization.instance().commands.ingameOnly));

                                }
                                break;
                            case BOTH:

                                try {
                                    final ServerPlayer player = source.getPlayerOrException();
                                    if (!mcSubCommand.needsOP()) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));

                                        source.sendSuccess(() -> {
                                            try {
                                                return ComponentArgument.textComponent().parse(new StringReader(txt));
                                            } catch (CommandSyntaxException ignored) {
                                                return null;
                                            }
                                        }, false);

                                    } else if (source.hasPermission(4)) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));


                                        source.sendSuccess(() -> {
                                            try {
                                                return ComponentArgument.textComponent().parse(new StringReader(txt));
                                            } catch (CommandSyntaxException ignored) {
                                                return null;
                                            }
                                        }, false);

                                    } else {
                                        source.sendFailure(net.minecraft.network.chat.Component.nullToEmpty(Localization.instance().commands.noPermission));
                                    }

                                } catch (CommandSyntaxException e) {
                                    final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, null));

                                    source.sendSuccess(() -> {
                                        try {
                                            return ComponentArgument.textComponent().parse(new StringReader(txt));
                                        } catch (CommandSyntaxException ignored) {
                                            return null;
                                        }
                                    }, false);
                                }
                                break;
                        }
                    }
                    ev.setCanceled(true);
                }
            }
        }

    }

    @SubscribeEvent
    public void serverStopping(ServerStoppedEvent ev) {
        if (INSTANCE != null) {
            ev.getServer().executeBlocking(() -> {
                INSTANCE.stopThreads();
                if (!ev.getServer().isRunning() && !Localization.instance().serverStopped.isBlank())
                    if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.stopMessages.asEmbed) {
                        if (!Configuration.instance().embedMode.stopMessages.customJSON.isBlank()) {
                            final EmbedBuilder b = Configuration.instance().embedMode.stopMessages.toEmbedJson(Configuration.instance().embedMode.stopMessages.customJSON);
                            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                        } else
                            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(Configuration.instance().embedMode.stopMessages.toEmbed().setDescription(Localization.instance().serverStopped).build()));
                    } else
                        DiscordIntegration.INSTANCE.sendMessage(Localization.instance().serverStopped);
                else if (ev.getServer().isRunning() && !Localization.instance().serverCrash.isBlank()) {
                    if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.stopMessages.asEmbed) {
                        if (!Configuration.instance().embedMode.stopMessages.customJSON.isBlank()) {
                            final EmbedBuilder b = Configuration.instance().embedMode.stopMessages.toEmbedJson(Configuration.instance().embedMode.stopMessages.customJSON);
                            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                        } else
                            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(Configuration.instance().embedMode.stopMessages.toEmbed().setDescription(Localization.instance().serverStopped).build()));
                    } else
                        DiscordIntegration.INSTANCE.sendMessage(Localization.instance().serverCrash);
                }
                INSTANCE.kill(false);
                INSTANCE = null;
                this.stopped = true;
                LOGGER.info("Shut-down successfully!");
            });
        }
    }

    @SubscribeEvent
    public void chat(ServerChatEvent ev) {
        if (Localization.instance().discordChatMessage.isBlank()) return;
        if (!DiscordIntegration.INSTANCE.getServerInterface().playerHasPermissions(ev.getPlayer().getUUID(), MinecraftPermission.SEMD_MESSAGES, MinecraftPermission.USER))
            return;
        if (LinkManager.isPlayerLinked(ev.getPlayer().getUUID()) && LinkManager.getLink(null, ev.getPlayer().getUUID()).settings.hideFromDiscord)
            return;
        final net.minecraft.network.chat.Component msg = ev.getMessage();
        if (INSTANCE.callEvent((e) -> {
            if (e instanceof ForgeDiscordEventHandler) {
                return ((ForgeDiscordEventHandler) e).onMcChatMessage(ev);
            }
            return false;
        })) return;

        final String text = MessageUtils.escapeMarkdown(ev.getMessage().getString().replace("@everyone", "[at]everyone").replace("@here", "[at]here"));
        final MessageEmbed embed = ForgeMessageUtils.genItemStackEmbedIfAvailable(msg);
        if (INSTANCE != null) {
            GuildMessageChannel channel = INSTANCE.getChannel(Configuration.instance().advanced.chatOutputChannelID);
            if (channel == null) return;
            if (!Localization.instance().discordChatMessage.isBlank())
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.chatMessages.asEmbed) {
                    final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", ev.getPlayer().getUUID().toString()).replace("%uuid_dashless%", ev.getPlayer().getUUID().toString().replace("-", "")).replace("%name%", ev.getPlayer().getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.chatMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.chatMessages.toEmbedJson(Configuration.instance().embedMode.chatMessages.customJSON
                                .replace("%uuid%", ev.getPlayer().getUUID().toString())
                                .replace("%uuid_dashless%", ev.getPlayer().getUUID().toString().replace("-", ""))
                                .replace("%name%", ForgeMessageUtils.formatPlayerName(ev.getPlayer()))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%msg%", text)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(ev.getPlayer().getUUID()).getRGB())
                        );
                        INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        EmbedBuilder b = Configuration.instance().embedMode.chatMessages.toEmbed();
                        if (Configuration.instance().embedMode.chatMessages.generateUniqueColors)
                            b = b.setColor(TextColors.generateFromUUID(ev.getPlayer().getUUID()));
                        b = b.setAuthor(ForgeMessageUtils.formatPlayerName(ev.getPlayer()), null, avatarURL)
                                .setDescription(text);
                        INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    }
                } else
                    INSTANCE.sendMessage(ForgeMessageUtils.formatPlayerName(ev.getPlayer()), ev.getPlayer().getUUID().toString(), new DiscordMessage(embed, text, true), channel);
            if (!Configuration.instance().compatibility.disableParsingMentionsIngame) {
                final String json = net.minecraft.network.chat.Component.Serializer.toJson(msg);
                Component comp = GsonComponentSerializer.gson().deserialize(json);
                final String editedJson = GsonComponentSerializer.gson().serialize(MessageUtils.mentionsToNames(comp, channel.getGuild()));
                ev.setMessage(net.minecraft.network.chat.Component.Serializer.fromJson(editedJson));
            }
        }

    }

    @SubscribeEvent
    public void death(LivingDeathEvent ev) {
        if (Localization.instance().playerDeath.isBlank()) return;
        if (ev.getEntity() instanceof Player) {
            if (LinkManager.isPlayerLinked(ev.getEntity().getUUID()) && LinkManager.getLink(null, ev.getEntity().getUUID()).settings.hideFromDiscord)
                return;
            if (INSTANCE != null) {
                final net.minecraft.network.chat.Component deathMessage = ev.getSource().getLocalizedDeathMessage(ev.getEntity());
                final MessageEmbed embed = ForgeMessageUtils.genItemStackEmbedIfAvailable(deathMessage);
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.deathMessage.asEmbed) {
                    final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", ev.getEntity().getUUID().toString()).replace("%uuid_dashless%", ev.getEntity().getUUID().toString().replace("-", "")).replace("%name%", ev.getEntity().getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.deathMessage.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.deathMessage.toEmbedJson(Configuration.instance().embedMode.deathMessage.customJSON
                                .replace("%uuid%", ev.getEntity().getUUID().toString())
                                .replace("%uuid_dashless%", ev.getEntity().getUUID().toString().replace("-", ""))
                                .replace("%name%", ForgeMessageUtils.formatPlayerName(ev.getEntity()))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%deathMessage%", ChatFormatting.stripFormatting(deathMessage.getString()).replace(ForgeMessageUtils.formatPlayerName(ev.getEntity()) + " ", ""))
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(ev.getEntity().getUUID()).getRGB())
                        );
                        if (embed != null) {
                            b.addBlankField(false);
                            b.addField(embed.getTitle() + " *(" + embed.getFooter().getText() + ")*", embed.getDescription(), false);
                        }
                        INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        final EmbedBuilder b = Configuration.instance().embedMode.deathMessage.toEmbed();
                        b.setDescription(":skull: " + Localization.instance().playerDeath.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.getEntity())).replace("%msg%", ChatFormatting.stripFormatting(deathMessage.getString()).replace(ForgeMessageUtils.formatPlayerName(ev.getEntity()) + " ", "")));
                        if (embed != null) {
                            b.addBlankField(false);
                            b.addField(embed.getTitle() + " *(" + embed.getFooter().getText() + ")*", embed.getDescription(), false);
                        }
                        INSTANCE.sendMessage(new DiscordMessage(b.build()), INSTANCE.getChannel(Configuration.instance().advanced.deathsChannelID));
                    }
                } else
                    INSTANCE.sendMessage(new DiscordMessage(embed, Localization.instance().playerDeath.replace("%player%", ForgeMessageUtils.formatPlayerName(ev.getEntity())).replace("%msg%", ChatFormatting.stripFormatting(deathMessage.getString()).replace(ForgeMessageUtils.formatPlayerName(ev.getEntity()) + " ", ""))), INSTANCE.getChannel(Configuration.instance().advanced.deathsChannelID));
            }
        }
    }

    @SubscribeEvent
    public void playerLeave(PlayerEvent.PlayerLoggedOutEvent ev) {
        if (stopped) return; //Try to fix player leave messages after stop!
        if (Localization.instance().playerLeave.isBlank()) return;
        final Player player = ev.getEntity();
        final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", player.getUUID().toString()).replace("%uuid_dashless%", player.getUUID().toString().replace("-", "")).replace("%name%", player.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
        if (DiscordIntegration.INSTANCE != null && !DiscordIntegrationMod.timeouts.contains(player.getUUID())) {
            if (!Localization.instance().playerLeave.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerLeaveMessages.asEmbed) {
                    if (!Configuration.instance().embedMode.playerLeaveMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbedJson(Configuration.instance().embedMode.playerLeaveMessages.customJSON
                                .replace("%uuid%", player.getUUID().toString())
                                .replace("%uuid_dashless%", player.getUUID().toString().replace("-", ""))
                                .replace("%name%", ForgeMessageUtils.formatPlayerName(player))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(player.getUUID()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbed();
                        b = b.setAuthor(ForgeMessageUtils.formatPlayerName(player), null, avatarURL)
                                .setDescription(Localization.instance().playerLeave.replace("%player%", ForgeMessageUtils.formatPlayerName(player)));
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerLeave.replace("%player%", ForgeMessageUtils.formatPlayerName(player)));
            }
        } else if (DiscordIntegration.INSTANCE != null && DiscordIntegrationMod.timeouts.contains(player.getUUID())) {
            if (!Localization.instance().playerTimeout.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerLeaveMessages.asEmbed) {
                    EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbed();
                    b = b.setAuthor(ForgeMessageUtils.formatPlayerName(player), null, avatarURL)
                            .setDescription(Localization.instance().playerTimeout.replace("%player%", ForgeMessageUtils.formatPlayerName(player)));
                    DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerTimeout.replace("%player%", ForgeMessageUtils.formatPlayerName(player)));
            }
            DiscordIntegrationMod.timeouts.remove(player.getUUID());
        }
    }
}
