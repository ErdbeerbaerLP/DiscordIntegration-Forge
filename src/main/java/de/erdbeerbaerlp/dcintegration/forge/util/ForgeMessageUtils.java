package de.erdbeerbaerlp.dcintegration.forge.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
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

import java.util.Arrays;

public class ForgeMessageUtils extends MessageUtils {
    private static final JsonParser p = new JsonParser();

    private static final IForgeRegistry<Item> itemreg = GameRegistry.findRegistry(Item.class);

    public static String formatPlayerName(Entity p) {
        return formatPlayerName(p, true);
    }

    public static String formatPlayerName(Entity p, boolean chatFormat) {
        final String discordName = getDiscordName(p.getUniqueID());
        if (discordName != null)
            return discordName;
        if (p.getDisplayName().getString().isEmpty())
            return p.getName().getString();
        else
            return TextFormatting.getTextWithoutFormattingCodes(p.getDisplayName().getString());
    }

    /**
     * Attempts to generate an {@link MessageEmbed} showing item info from an {@link ITextComponent} instance
     *
     * @param component The TextComponent to scan for item info
     * @return an {@link MessageEmbed} when there was an Item info, or {@link null} if there was no item info OR the item info was disabled
     */
    public static MessageEmbed genItemStackEmbedIfAvailable(final ITextComponent component) {
        if (!Configuration.instance().forgeSpecific.sendItemInfo) return null;
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
                                            Registry.ENCHANTMENT.getOptional(ResourceLocation.tryCreate(compoundnbt.getString("id"))).ifPresent((ench) -> {
                                                if (compoundnbt.get("lvl") != null) {
                                                    final int level;
                                                    if (compoundnbt.get("lvl") instanceof StringNBT) {
                                                        level = Integer.parseInt(compoundnbt.getString("lvl").replace("s", ""));
                                                    } else
                                                        level = compoundnbt.getInt("lvl") == 0 ? compoundnbt.getShort("lvl") : compoundnbt.getInt("lvl");
                                                    tooltip.append(TextFormatting.getTextWithoutFormattingCodes(ench.getDisplayName(level).getString())).append("\n");
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
                                                if (formattingSymbols.endsWith("_"))
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


}
