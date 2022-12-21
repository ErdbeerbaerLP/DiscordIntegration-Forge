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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class ForgeMessageUtils extends MessageUtils {

    private static final IForgeRegistry<Item> itemreg = ForgeRegistries.ITEMS;

    public static String formatPlayerName(Map.Entry<UUID, String> p) {
        return formatPlayerName(p, true);
    }

    public static String formatPlayerName(Map.Entry<UUID, String> p, @SuppressWarnings("unused") boolean chatFormat) {
        return ChatFormatting.stripFormatting(p.getValue());
    }

    /**
     * Attempts to generate an {@link MessageEmbed} showing item info from an {@link Component} instance
     *
     * @param component The TextComponent to scan for item info
     * @return an {@link MessageEmbed} when there was an Item info, or {@link null} if there was no item info OR the item info was disabled
     */
    public static MessageEmbed genItemStackEmbedIfAvailable(final Component component) {
        if (!Configuration.instance().forgeSpecific.sendItemInfo) return null;
        final JsonObject json = JsonParser.parseString(Component.Serializer.toJson(component)).getAsJsonObject();
        if (json.has("with")) {
            final JsonArray args = json.getAsJsonArray("with");
            for (JsonElement el : args) {
                if (el instanceof JsonObject arg1) {
                    if (arg1.has("hoverEvent")) {
                        final JsonObject hoverEvent = arg1.getAsJsonObject("hoverEvent");
                        if (hoverEvent.has("action") && hoverEvent.get("action").getAsString().equals("show_item") && hoverEvent.has("contents")) {
                            if (hoverEvent.getAsJsonObject("contents").has("tag")) {
                                final JsonObject item = hoverEvent.getAsJsonObject("contents").getAsJsonObject();
                                try {
                                    final ItemStack is = new ItemStack(itemreg.getValue(new ResourceLocation(item.get("id").getAsString())));
                                    if (item.has("tag")) {
                                        final CompoundTag tag = (CompoundTag) NbtTagArgument.nbtTag().parse(new StringReader(item.get("tag").getAsString()));
                                        is.setTag(tag);
                                    }
                                    final CompoundTag itemTag = is.getOrCreateTag();
                                    final EmbedBuilder b = new EmbedBuilder();
                                    String title = is.hasCustomHoverName() ? is.getDisplayName().getString() : Component.translatable(is.getItem().getDescriptionId()).getString();
                                    if (title.isEmpty())
                                        title = is.getItem().getDescriptionId();
                                    else
                                        b.setFooter(is.getItem().getDescriptionId());
                                    b.setTitle(title);
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
                                        for (int i = 0; i < is.getEnchantmentTags().size(); ++i) {
                                            final CompoundTag compoundnbt = is.getEnchantmentTags().getCompound(i);
                                            final Enchantment ench = Enchantment.byId(compoundnbt.getInt("id"));
                                            if (compoundnbt.get("lvl") != null) {
                                                final int level;
                                                if (compoundnbt.get("lvl") instanceof StringTag) {
                                                    level = Integer.parseInt(compoundnbt.getString("lvl").replace("s", ""));
                                                } else
                                                    level = compoundnbt.getInt("lvl") == 0 ? compoundnbt.getShort("lvl") : compoundnbt.getInt("lvl");
                                                tooltip.append(ChatFormatting.stripFormatting(ench.getFullname(level).getString())).append("\n");
                                            }
                                        }
                                    }
                                    //Add Lores
                                    final ListTag list = itemTag.getCompound("display").getList("Lore", 8);
                                    list.forEach((nbt) -> {
                                        try {
                                            if (nbt instanceof StringTag) {
                                                final Component comp = ComponentArgument.textComponent().parse(new StringReader(nbt.getAsString()));
                                                tooltip.append("_").append(comp.getString()).append("_\n");
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
        final Map.Entry<UUID, String> e = new DefaultMapEntry<>(p.getUUID(), p.getDisplayName().getString().isEmpty() ? p.getName().getString() : p.getDisplayName().getString());
        return formatPlayerName(e);
    }
}
