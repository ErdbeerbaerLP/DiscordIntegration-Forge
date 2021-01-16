package de.erdbeerbaerlp.dcintegration.common.util;

import net.kyori.adventure.text.format.TextColor;

import javax.annotation.Nonnull;
import java.awt.*;

public class TextColors {


    /**
     * Color used for mentions / pings ingame
     */
    public static final TextColor PING = TextColor.color(209, 170, 63);

    /**
     * Converts the given {@link Color} to an {@link TextColor}
     * @param c {@link} Color to convert
     * @return Converted {@link TextColor}
     */
    @Nonnull
    public static TextColor of(@Nonnull Color c) {
        return TextColor.color(c.getRed(), c.getGreen(), c.getBlue());
    }

}
