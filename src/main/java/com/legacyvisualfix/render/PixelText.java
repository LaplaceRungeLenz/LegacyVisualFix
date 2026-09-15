package com.legacyvisualfix.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

/** Original 5x7 Latin glyphs. Independent of Minecraft's reloading font/texture manager. */
public final class PixelText {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 /-.:?";
    private static final String[] ROWS = { "0E11111F111111", "1E11111E11111E", "0F10101010100F", "1E11111111111E",
        "1F10101E10101F", "1F10101E101010", "0F10101711110F", "1111111F111111", "0E04040404040E", "0702020212120C",
        "11121418141211", "1010101010101F", "111B1515111111", "11191513111111", "0E11111111110E", "1E11111E101010",
        "0E11111115120D", "1E11111E141211", "0F10100E01011E", "1F040404040404", "1111111111110E", "11111111110A04",
        "11111115151B11", "11110A040A1111", "11110A04040404", "1F01020408101F", "0E11131519110E", "040C040404040E",
        "0E11010204081F", "1E01010E01011E", "02060A121F0202", "1F10101E01011E", "0E10101E11110E", "1F010204080808",
        "0E11110E11110E", "0E11110F01010E", "00000000000000", "01010204081010", "0000001F000000", "00000000000C0C",
        "000C0C000C0C00", "0E110102040004" };

    private PixelText() {}

    public static int width(String text) {
        return Math.max(1, text.length() * 6 - 1);
    }

    public static BufferedImage image(String text) {
        BufferedImage image = new BufferedImage(width(text), 9, BufferedImage.TYPE_INT_ARGB);
        draw(image, text, 0, 0);
        return image;
    }

    public static void draw(BufferedImage image, String text, int x, int y) {
        String upper = text.toUpperCase(Locale.ROOT);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
            for (int i = 0; i < upper.length(); i++) {
                char ch = upper.charAt(i);
                int index = CHARACTERS.indexOf(ch);
                if (index < 0) {
                    g.drawString(String.valueOf(ch), x + i * 6, y + 7);
                    continue;
                }
                for (int row = 0; row < 7; row++) {
                    int bits = Integer.parseInt(ROWS[index].substring(row * 2, row * 2 + 2), 16);
                    for (int col = 0; col < 5; col++) {
                        if ((bits & (1 << (4 - col))) != 0) g.fillRect(x + i * 6 + col, y + row, 1, 1);
                    }
                }
            }
        } finally {
            g.dispose();
        }
    }
}
