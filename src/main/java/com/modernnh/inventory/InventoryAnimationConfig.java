package com.modernnh.inventory;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class InventoryAnimationConfig {

    public static boolean enabled = true;
    public static int durationMs = 250;
    public static int distance = 0;

    private InventoryAnimationConfig() {}

    public static void load(File directory) {
        Configuration config = new Configuration(new File(directory, "modernnh/inventory.cfg"));
        config.load();
        enabled = config.getBoolean(
            "enabled",
            "inventory",
            true,
            "Animate vanilla survival and creative inventory only. Restart to apply.");
        durationMs = config.getInt(
            "durationMs",
            "inventory",
            250,
            0,
            1000,
            "Entrance duration in milliseconds. 0 disables animation. Restart to apply.");
        distance = config.getInt(
            "distance",
            "inventory",
            0,
            0,
            1000,
            "Slide distance in GUI pixels. 0 starts below the screen. Restart to apply.");
        if (config.hasChanged()) config.save();
    }
}
