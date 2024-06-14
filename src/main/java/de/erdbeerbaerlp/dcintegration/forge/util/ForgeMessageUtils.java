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
import de.erdbeerbaerlp.dcintegration.forge.util.accessors.ShowInTooltipAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class ForgeMessageUtils extends MessageUtils {

    private static final DefaultedRegistry<Item> itemreg = BuiltInRegistries.ITEM;

    public static String formatPlayerName(Map.Entry<UUID, String> p) {
        return formatPlayerName(p, true);
    }

    public static String formatPlayerName(Map.Entry<UUID, String> p, boolean chatFormat) {
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
        JsonObject json;
        try {
            final JsonElement jsonElement = JsonParser.parseString(Component.Serializer.toJson(component, ServerLifecycleHooks.getCurrentServer().registryAccess()));
            if (jsonElement.isJsonObject())
                json = jsonElement.getAsJsonObject();
            else return null;
        } catch (final IllegalStateException ex) {
            ex.printStackTrace();
            return null;
        }
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
                                    final ItemStack is = new ItemStack(itemreg.get(ResourceLocation.parse(item.get("id").getAsString())));
                                    ItemStack.parse(ServerLifecycleHooks.getCurrentServer().registryAccess(), NbtTagArgument.nbtTag().parse(new StringReader(item.getAsString())));
                                    final DataComponentMap itemTag = is.getComponents();
                                    final EmbedBuilder b = new EmbedBuilder();
                                    Component title = itemTag.getOrDefault(DataComponents.CUSTOM_NAME, Component.translatable(is.getItemHolder().getRegisteredName(), is.getDisplayName().getString(), null));
                                    if (title.toString().isEmpty())
                                        title = Component.translatable(is.getItemHolder().getRegisteredName());
                                    else
                                        b.setFooter(is.getItemHolder().getRegisteredName().toString());
                                    b.setTitle(title.getString());
                                    final StringBuilder tooltip = new StringBuilder();
                                    boolean[] flags = new boolean[6]; // Enchantments, Modifiers, Unbreakable, CanDestroy, CanPlace, Other
                                    Arrays.fill(flags, false); // Set everything visible

                                    //Add Enchantments
                                    if (itemTag.has(DataComponents.ENCHANTMENTS)) {
                                        final ItemEnchantments e = itemTag.get(DataComponents.ENCHANTMENTS);
                                        if (((ShowInTooltipAccessor) e).discordIntegrationFabric$showsInTooltip())
                                            for (Object2IntMap.Entry<Holder<Enchantment>> ench : e.entrySet()) {
                                                tooltip.append(ChatFormatting.stripFormatting(ench.getKey().value().getFullname(ench.getKey(),e.getLevel(ench.getKey())).getString())).append("\n");
                                            }
                                    }
                                    if(itemTag.has(DataComponents.LORE)) {
                                        final ItemLore l = itemTag.get(DataComponents.LORE);
                                        //Add Lores
                                        for (Component line : l.lines()) {
                                            tooltip.append("_").append(line.getString()).append("_\n");
                                        }
                                    }
                                    //Add 'Unbreakable' Tag
                                    if(itemTag.has(DataComponents.UNBREAKABLE)){
                                        final Unbreakable unb = itemTag.get(DataComponents.UNBREAKABLE);
                                        if (unb.showInTooltip())
                                            tooltip.append("Unbreakable\n");
                                    }
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
        final Map.Entry<UUID, String> e = new DefaultMapEntry(p.getUUID(), p.getDisplayName().getString().isEmpty() ? p.getName().getContents() : p.getDisplayName().getString());
        return formatPlayerName(e);
    }
}
