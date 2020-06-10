package de.erdbeerbaerlp.dcintegration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.erdbeerbaerlp.dcintegration.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.storage.PlayerSettings;
import me.vankka.reserializer.discord.DiscordSerializer;
import me.vankka.reserializer.minecraft.MinecraftSerializer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static de.erdbeerbaerlp.dcintegration.DiscordIntegration.LOGGER;


public class Utils {
    static final JsonParser p = new JsonParser();
    private static final IForgeRegistry<Item> itemreg = GameRegistry.findRegistry(Item.class);
    private static final String updateCheckerURL = "https://raw.githubusercontent.com/ErdbeerbaerLP/Discord-Chat-Integration/1.15/update_checker.json";

    /**
     * Attempts to generate an {@link MessageEmbed} showing item info from an {@link ITextComponent} instance
     *
     * @param component The TextComponent to scan for item info
     * @return an {@link MessageEmbed} when there was an Item info, or {@link null} if there was no item info OR the item info was disabled
     */
    public static MessageEmbed genItemStackEmbedIfAvailable(final ITextComponent component) {
        if (!Configuration.INSTANCE.sendItemInfo.get()) return null;
        final JsonObject json = p.parse(ITextComponent.Serializer.toJson(component)).getAsJsonObject();
        if (json.has("with")) {
            final JsonArray args = json.getAsJsonArray("with");
            for (JsonElement el : args) {
                if (el instanceof JsonObject) {
                    JsonObject arg1 = (JsonObject) el;
                    if (arg1.has("hoverEvent")) {
                        final JsonObject hoverEvent = arg1.getAsJsonObject("hoverEvent");
                        if (hoverEvent.has("action") && hoverEvent.get("action").getAsString().equals("show_item") && hoverEvent.has("value")) {
                            if (hoverEvent.getAsJsonObject("value").has("text")) {
                                final String it = hoverEvent.getAsJsonObject("value").get("text").getAsString();
                                final JsonObject item = p.parse(it).getAsJsonObject();
                                try {
                                    final ItemStack is = new ItemStack(itemreg.getValue(new ResourceLocation(item.get("id").getAsString())));
                                    if (item.has("tag")) {
                                        final CompoundNBT tag = JsonToNBT.getTagFromJson(item.get("tag").getAsJsonObject().toString());
                                        is.setTag(tag);
                                    }
                                    final CompoundNBT itemTag = is.getOrCreateTag();
                                    final EmbedBuilder b = new EmbedBuilder();
                                    b.setTitle(is.hasDisplayName() ? is.getDisplayName().getUnformattedComponentText() : new TranslationTextComponent(is.getTranslationKey()).getUnformattedComponentText());
                                    b.setFooter(is.getItem().getRegistryName().toString());
                                    final StringBuilder tooltip = new StringBuilder();
                                    boolean[] flags = new boolean[6]; // Enchantments, Modifiers, Unbreakable, CanDestroy, CanPlace, Other
                                    Arrays.fill(flags, false); // Set everything visible

                                    if (itemTag.contains("HideFlags")) {
                                        final int input = (itemTag.getInt("HideFlags"));
                                        for (int i = 0; i < flags.length; i++) {
                                            flags[i] = (input & (1 << i)) != 0;
                                        }
                                    }
                                    //Add Enchantments
                                    if (!flags[0]) {
                                        //Implementing this code myself because the original is broken
                                        for (int i = 0; i < is.getEnchantmentTagList().size(); ++i) {
                                            final CompoundNBT compoundnbt = is.getEnchantmentTagList().getCompound(i);
                                            Registry.ENCHANTMENT.getValue(ResourceLocation.tryCreate(compoundnbt.getString("id"))).ifPresent((ench) -> {
                                                if (compoundnbt.get("lvl") != null) {
                                                    final int level;
                                                    if (compoundnbt.get("lvl") instanceof StringNBT) {
                                                        level = Integer.parseInt(compoundnbt.getString("lvl").replace("s", ""));
                                                    } else
                                                        level = compoundnbt.getInt("lvl") == 0 ? compoundnbt.getShort("lvl") : compoundnbt.getInt("lvl");
                                                    tooltip.append(TextFormatting.getTextWithoutFormattingCodes(ench.getDisplayName(level).getFormattedText())).append("\n");
                                                }
                                            });
                                        }/* Broken Code
                                        EnchantmentHelper.getEnchantments(is).forEach((ench, lvl) -> {
                                        tooltip.append(TextFormatting.getTextWithoutFormattingCodes(ench.getDisplayName(lvl).getFormattedText())).append("\n");
                                        });*/
                                    }
                                    //Add Lores
                                    final ListNBT list = itemTag.getCompound("display").getList("Lore", 8);
                                    list.forEach((nbt) -> {
                                        if (nbt instanceof StringNBT) {
                                            final String txt = TextFormatting.getTextWithoutFormattingCodes(nbt.getString());
                                            tooltip.append("_").append(txt, 1, txt.length() - 1).append("_\n");
                                        } else if (nbt instanceof CompoundNBT) {
                                            final CompoundNBT comp = (CompoundNBT) nbt;
                                            final String txt = TextFormatting.getTextWithoutFormattingCodes(comp.getString("text"));
                                            String formattingSymbols = "";
                                            if (comp.getBoolean("bold")) formattingSymbols = formattingSymbols + "**";
                                            if (comp.getBoolean("underline"))
                                                formattingSymbols = formattingSymbols + "__";
                                            if (comp.getBoolean("italic")) {
                                                if (formattingSymbols.substring(formattingSymbols.length() - 1).equals("_"))
                                                    formattingSymbols = formattingSymbols + "*";
                                                else
                                                    formattingSymbols = formattingSymbols + "_";
                                            }
                                            if (comp.getBoolean("strikethrough"))
                                                formattingSymbols = formattingSymbols + "~~";
                                            tooltip.append(formattingSymbols).append(txt, 1, txt.length() - 1).append(StringUtils.reverse(formattingSymbols)).append("\n");
                                        }
                                    });
                                    //Add 'Unbreakable' Tag
                                    if (!flags[2] && itemTag.contains("Unbreakable") && itemTag.getBoolean("Unbreakable"))
                                        tooltip.append("Unbreakable\n");

                                    b.setDescription(tooltip.toString());
                                    return b.build();
                                } catch (CommandSyntaxException ignored) {
                                    //Just go on and ignore it
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String formatPlayerName(Entity p) {
        return formatPlayerName(p, true);
    }

    static String[] makeStringArray(final JsonArray channelID) {
        final String[] out = new String[channelID.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = channelID.get(i).getAsString();
        }
        return out;
    }

    public static String getFullUptime() {
        if (DiscordIntegration.started == 0) {
            return "?????";
        }
        final Duration duration = Duration.between(Instant.ofEpochMilli(DiscordIntegration.started), Instant.now());
        return DurationFormatUtils.formatDuration(duration.toMillis(), Configuration.INSTANCE.uptimeFormat.get());
    }

    public static int getUptimeSeconds() {
        long diff = new Date().getTime() - DiscordIntegration.started;
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

    public static String formatPlayerName(Entity p, boolean chatFormat) {
        /*if (Loader.isModLoaded("ftbutilities") && p instanceof EntityPlayer) {
            final FTBUtilitiesPlayerData d = FTBUtilitiesPlayerData.get(Universe.get().getPlayer(p));
            final String nick = (Configuration.FTB_UTILITIES.CHAT_FORMATTING && chatFormat) ? d.getNameForChat((EntityPlayerMP) p).getUnformattedText().replace("<", "").replace(">", "").trim() : d.getNickname().trim();
            if (!nick.isEmpty()) return nick;
        }*/
        try {
            if (Configuration.INSTANCE.allowLink.get() && PlayerLinkController.isPlayerLinked(p.getUniqueID())) {
                final PlayerSettings settings = PlayerLinkController.getSettings(null, p.getUniqueID());
                if (settings.useDiscordNameInChannel) {
                    return DiscordIntegration.discord_instance.jda.getTextChannelById(Configuration.INSTANCE.botChannel.get()).getGuild().getMember(DiscordIntegration.discord_instance.jda.getUserById(PlayerLinkController.getDiscordFromPlayer(p.getUniqueID()))).getEffectiveName();
                }
            }
        } catch (NullPointerException ignored) {
        }
        if (p.getDisplayName().getFormattedText().isEmpty())
            return p.getName().getFormattedText();
        else
            return TextFormatting.getTextWithoutFormattingCodes(p.getDisplayName().getFormattedText());
    }

    public static String escapeMarkdown(String in) {
        return in.replace("(?<!\\\\)[`*_|~]/g", "\\\\$0");
    }

    public static String escapeMarkdownCodeBlocks(String in) {
        return in.replace("(?<!\\\\)`/g", "\\\\$0");
    }

    public static String convertMarkdownToMCFormatting(String in) {
        if (!Configuration.INSTANCE.convertCodes.get()) return in;
        in = escapeMarkdownCodeBlocks(in);
        try {
            return LegacyComponentSerializer.INSTANCE.serialize(MinecraftSerializer.INSTANCE.serialize(in));
        } catch (NullPointerException | ConcurrentModificationException ex) {
            ex.printStackTrace();
            return in;
        }
    }

    public static String convertMCToMarkdown(String in) {
        if (!Configuration.INSTANCE.convertCodes.get()) {
            if (Configuration.INSTANCE.formattingCodesToDiscord.get()) return in;
            else return TextFormatting.getTextWithoutFormattingCodes(in);
        }
        in = escapeMarkdownCodeBlocks(in);
        try {
            return DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.INSTANCE.deserialize(in));
        } catch (NullPointerException | ConcurrentModificationException ex) {
            ex.printStackTrace();
            return in;
        }
    }

    static void runUpdateCheck() {
        if (!Configuration.INSTANCE.enableUpdateChecker.get()) return;
        final StringBuilder changelog = new StringBuilder();
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(updateCheckerURL).openConnection();
            conn.setRequestMethod("GET");
            final InputStreamReader r = new InputStreamReader(conn.getInputStream());
            final JsonArray parse = p.parse(r).getAsJsonArray();
            if (parse == null) {
                LOGGER.error("Could not check for updates");
                return;
            }
            final AtomicBoolean shouldNotify = new AtomicBoolean(false);
            final AtomicInteger versionsBehind = new AtomicInteger();
            parse.forEach((elm) -> {
                if (elm != null && elm.isJsonObject()) {
                    final JsonObject versionDetails = elm.getAsJsonObject();
                    final String version = versionDetails.get("version").getAsString();
                    try {
                        if (Integer.parseInt(version.replace(".", "")) > Integer.parseInt(DiscordIntegration.VERSION.replace(".", ""))) {
                            versionsBehind.getAndIncrement();
                            changelog.append("\n").append(version).append(":\n").append(versionDetails.get("changelog").getAsString()).append("\n");
                            if (!shouldNotify.get()) {
                                if (Configuration.ReleaseType.getFromName(versionDetails.get("type").getAsString()).value >= Configuration.INSTANCE.updateCheckerMinimumReleaseType.get().value)
                                    shouldNotify.set(true);
                            }
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            });
            final String changelogString = changelog.toString();
            if (shouldNotify.get()) {
                LOGGER.info("[Discord Integration] Updates available! You are " + versionsBehind.get() + " version" + (versionsBehind.get() == 1 ? "" : "s") + " behind\nChangelog since last update:\n" + changelogString);
            }
        } catch (IOException e) {
            LOGGER.error("Could not check for updates");
            e.printStackTrace();
        }
    }
}

