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
    public final boolean showDetails;
    public final boolean showLogo;
    public final int fadeInMs;
    public final int fadeOutMs;
    public final String logo;
    public final String title;
    public final double logoX;
    public final double logoY;
    public final double logoHeight;
    public final double titleY;
    public final double barX;
    public final double barY;
    public final int barWidth;
    public final double barWidthFraction;
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
        showDetails = bool(p, "showDetails", false);
        showLogo = bool(p, "showLogo", true);
        fadeInMs = (int) number(p, "animation.fadeInMs", 400, 0, 2000);
        fadeOutMs = (int) number(p, "animation.fadeOutMs", 600, 0, 2000);
        logo = texture(p, "texture.logo", "modernnh:textures/gui/logo.png");
        String requestedTitle = p.getProperty("title", "GT NEW HORIZONS")
            .trim();
        title = requestedTitle.length() > 128 ? "GT NEW HORIZONS" : requestedTitle;
        logoX = number(p, "logo.x", 0.5, 0, 1);
        logoY = number(p, "logo.y", 0.38, 0, 1);
        logoHeight = number(p, "logo.height", 0.37, 0.05, 0.8);
        titleY = number(p, "title.y", 0.62, 0, 1);
        barX = number(p, "bar.x", 0.5, 0, 1);
        barY = number(p, "bar.y", 0.71, 0, 1);
        barWidth = (int) number(p, "bar.width", 240, 16, 4096);
        barWidthFraction = number(p, "bar.widthFraction", p.containsKey("bar.width") ? 0 : 0.4, 0, 1);
        barHeight = (int) number(p, "bar.height", 6, 2, 256);
        backgroundColor = color(p, "color.background", 0xFF101E2C);
        trackColor = color(p, "color.track", 0xFFFFFFFF);
        fillColor = color(p, "color.fill", 0xFFFFFFFF);
        textColor = color(p, "color.text", 0xFFE9E6D9);
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
