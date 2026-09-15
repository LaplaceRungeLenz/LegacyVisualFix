package com.modernnh.ui;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** Independent client settings; restart to apply. */
public final class UiEffectsConfig {

    public static boolean enabled = true;
    public static boolean hotbar = true;
    public static boolean hover = true;
    public static boolean carried = true;
    public static boolean matching = true;
    public static boolean trails = true;
    public static float hoverScale = 1.2f;
    public static float carriedScale = 1.2f;
    public static float rotation = 18;
    public static float floatAmplitude = 0.65f;
    public static float speed = 18;
    public static int maxParticles = 128;
    public static int particleRate = 60;
    public static String[] excludedScreens = new String[0];

    private UiEffectsConfig() {}

    public static void load(File directory) {
        Configuration c = new Configuration(new File(directory, "modernnh/ui.cfg"));
        c.load();
        enabled = c.getBoolean("enabled", "effects", true, "Enable client UI effects. Restart to apply all settings.");
        hotbar = c
            .getBoolean("hotbar", "effects", true, "Smooth the hotbar selector; actual selection changes immediately.");
        hover = c.getBoolean("hover", "effects", true, "Scale hovered real inventory items.");
        carried = c.getBoolean("carried", "effects", true, "Scale and gently rotate the mouse-carried item.");
        matching = c
            .getBoolean("matching", "effects", true, "Float matching item, metadata and NBT stacks. Ignores count.");
        trails = c.getBoolean("trails", "effects", true, "Colored trails for non-common rarity mouse-carried items.");
        hoverScale = c.getFloat("hoverScale", "effects", 1.2f, 1, 1.6f, "Hovered item size multiplier.");
        carriedScale = c.getFloat("carriedScale", "effects", 1.2f, 1, 1.6f, "Mouse-carried item size multiplier.");
        rotation = c.getFloat("rotationDegrees", "effects", 18, 0, 25, "Maximum carried item tilt in degrees.");
        floatAmplitude = c
            .getFloat("floatAmplitude", "effects", 0.65f, 0, 2, "Matching item vertical motion in GUI pixels.");
        speed = c.getFloat("responseSpeed", "effects", 18, 1, 40, "Hotbar and hover response. Higher is faster.");
        maxParticles = c.getInt("maxParticles", "effects", 128, 0, 512, "Hard cap on live GUI trail particles.");
        particleRate = c
            .getInt("particleRate", "effects", 60, 0, 120, "Maximum trail emission per second while moving.");
        excludedScreens = c.getStringList(
            "excludedScreens",
            "effects",
            new String[0],
            "Exact full GUI class names to exclude. No wildcards.");
        if (!Float.isFinite(hoverScale)) hoverScale = 1.2f;
        if (!Float.isFinite(carriedScale)) carriedScale = 1.2f;
        if (!Float.isFinite(rotation)) rotation = 18;
        if (!Float.isFinite(floatAmplitude)) floatAmplitude = 0.65f;
        if (!Float.isFinite(speed)) speed = 18;
        if (c.hasChanged()) c.save();
    }
}
