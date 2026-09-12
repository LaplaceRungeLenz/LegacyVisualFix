package com.modernnh.theme;

import java.util.Locale;
import java.util.Properties;

/** Immutable snapshot. Invalid individual values fall back independently. Colors are ARGB. */
public final class Theme {

    public enum Fit {
        COVER,
        CONTAIN,
        STRETCH
    }

    public final boolean enabled;
    public final boolean showText;
    public final double barX;
    public final double barY;
    public final int barWidth;
    public final int barHeight;
    public final int backgroundColor;
    public final int trackColor;
    public final int fillColor;
    public final int textColor;
    public final String background;
    public final String track;
    public final String fill;
    public final Fit fit;

    private Theme(Properties p) {
        enabled = bool(p, "enabled", true);
        showText = bool(p, "showText", true);
        barX = number(p, "bar.x", 0.5, 0, 1);
        barY = number(p, "bar.y", 0.75, 0, 1);
        barWidth = (int) number(p, "bar.width", 240, 16, 4096);
        barHeight = (int) number(p, "bar.height", 12, 2, 256);
        backgroundColor = color(p, "color.background", 0xFF101820);
        trackColor = color(p, "color.track", 0xFF2A3946);
        fillColor = color(p, "color.fill", 0xFF59C9A5);
        textColor = color(p, "color.text", 0xFFE5EEF4);
        background = texture(p, "texture.background", "modernnh:textures/gui/background.png");
        track = texture(p, "texture.track", "modernnh:textures/gui/bar_track.png");
        fill = texture(p, "texture.fill", "modernnh:textures/gui/bar_fill.png");
        Fit parsed;
        try {
            parsed = Fit.valueOf(
                p.getProperty("background.fit", "cover")
                    .trim()
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            parsed = Fit.COVER;
        }
        fit = parsed;
    }

    public static Theme parse(Properties properties) {
        return new Theme(properties);
    }

    private static double number(Properties p, String key, double fallback, double min, double max) {
        try {
            double value = Double.parseDouble(p.getProperty(key, ""));
            return !Double.isNaN(value) && !Double.isInfinite(value) && value >= min && value <= max ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int color(Properties p, String key, int fallback) {
        String value = p.getProperty(key, "")
            .trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (!value.matches("[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) return fallback;
        long parsed = Long.parseLong(value, 16);
        return (int) (value.length() == 6 ? parsed | 0xFF000000L : parsed);
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        String value = p.getProperty(key, "")
            .trim();
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return fallback;
    }

    private static String texture(Properties p, String key, String fallback) {
        String value = p.getProperty(key, fallback)
            .trim();
        // Empty explicitly selects solid-color rendering. No URI decoding or absolute local paths.
        if (value.isEmpty()) return value;
        if (!value.matches("[a-z0-9_.-]+:[a-zA-Z0-9_./-]+\\.png")) return fallback;
        String path = value.substring(value.indexOf(':') + 1);
        if (path.startsWith("/") || path.contains("..")) return fallback;
        return value;
    }
}
