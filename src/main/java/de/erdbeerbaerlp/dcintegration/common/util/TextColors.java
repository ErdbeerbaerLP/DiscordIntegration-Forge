package de.erdbeerbaerlp.dcintegration.common.util;

import net.kyori.adventure.text.format.TextColor;

import java.awt.*;

public class TextColors {
    public static final TextColor PING = TextColor.color(209, 170, 63);

    public static TextColor of(Color c) {
        return TextColor.color(c.getRed(), c.getGreen(), c.getBlue());
    }

}
