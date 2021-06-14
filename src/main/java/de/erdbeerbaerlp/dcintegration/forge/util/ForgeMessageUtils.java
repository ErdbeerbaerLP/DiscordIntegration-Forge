package de.erdbeerbaerlp.dcintegration.forge.util;

import com.google.gson.JsonParser;
import dcshadow.org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

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
        return null;
        // NOT IMPLEMENTED YET
    }

    public static String formatPlayerName(Entity p) {
        final Map.Entry<UUID, String> e = new DefaultMapEntry(p.getUniqueID(), p.getDisplayName().getUnformattedComponentText().isEmpty() ? p.getName() : p.getDisplayName().getUnformattedComponentText());
        return formatPlayerName(e);
    }
}
