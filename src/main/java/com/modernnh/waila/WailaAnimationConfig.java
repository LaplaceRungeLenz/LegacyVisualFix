package com.modernnh.waila;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** Separate from resource-reload themes; read once during client pre-initialization. */
public final class WailaAnimationConfig {

    public static boolean enabled = true;
    public static int durationMs = 150;

    private WailaAnimationConfig() {}

    public static void load(File configDirectory) {
        Configuration config = new Configuration(new File(configDirectory, "modernnh/waila-animation.cfg"));
        config.load();
        enabled = config
            .getBoolean("enabled", "animation", true, "Smooth Waila tooltip size changes. Restart to apply.");
        durationMs = config.getInt(
            "durationMs",
            "animation",
            150,
            0,
            1000,
            "Transition duration in milliseconds; 0 disables animation. Restart to apply.");
        if (config.hasChanged()) config.save();
    }
}
