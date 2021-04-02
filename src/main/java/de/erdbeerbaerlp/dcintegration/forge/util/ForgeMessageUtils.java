package de.erdbeerbaerlp.dcintegration.forge.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dcshadow.org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.command.arguments.ComponentArgument;
import net.minecraft.command.arguments.NBTCompoundTagArgument;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.*;
import net.minecraft.util.text.TextComponent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class ForgeMessageUtils extends MessageUtils {
    private static final JsonParser p = new JsonParser();

    private static final IForgeRegistry<Item> itemreg = GameRegistry.findRegistry(Item.class);

    public static String formatPlayerName(Map.Entry<UUID, String> p) {
        return formatPlayerName(p, true);
    }

    public static String formatPlayerName(Map.Entry<UUID, String> p, boolean chatFormat) {
            return TextFormatting.getTextWithoutFormattingCodes(p.getValue());
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
        System.out.println("Generating embed...");
        System.out.println("JSON: " + json);
        if (json.has("with")) {
            final JsonArray args = json.getAsJsonArray("with");
            for (JsonElement el : args) {
                if (el instanceof JsonObject) {
                    JsonObject arg1 = (JsonObject) el;
                    if (arg1.has("hoverEvent")) {
                        final JsonObject hoverEvent = arg1.getAsJsonObject("hoverEvent");
                        if (hoverEvent.has("action") && hoverEvent.get("action").getAsString().equals("show_item") && hoverEvent.has("contents")) {
                            if (hoverEvent.getAsJsonObject("contents").has("tag")) {
                                final JsonObject item = hoverEvent.getAsJsonObject("contents").getAsJsonObject();
                                try {
                                    final ItemStack is = new ItemStack(itemreg.getValue(new ResourceLocation(item.get("id").getAsString())));
                                    if (item.has("tag")) {
                                        final CompoundNBT tag = NBTCompoundTagArgument.nbt().parse(new StringReader(item.get("tag").getAsString()));
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
                                        }/*
                                        EnchantmentHelper.getEnchantments(is).forEach((ench, lvl) -> {
                                            tooltip.append(TextFormatting.getTextWithoutFormattingCodes(ench.getDisplayName(lvl).getUnformattedComponentText())).append("\n");
                                        });*/
                                    }
                                    //Add Lores
                                    final ListNBT list = itemTag.getCompound("display").getList("Lore", 8);
                                    list.forEach((nbt) -> {
                                        try {
                                            if (nbt instanceof StringNBT) {
                                                final TextComponent comp = (TextComponent) ComponentArgument.component().parse(new StringReader(nbt.getString()));
                                                tooltip.append("_").append(comp.getUnformattedComponentText()).append("_\n");
                                            }
                                        } catch (CommandSyntaxException e) {
                                            e.printStackTrace();
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
        final Map.Entry<UUID, String> e = new DefaultMapEntry(p.getUniqueID(), p.getDisplayName().getUnformattedComponentText().isEmpty() ? p.getName().getUnformattedComponentText() : p.getDisplayName().getUnformattedComponentText());
        return formatPlayerName(e);
    }
}
