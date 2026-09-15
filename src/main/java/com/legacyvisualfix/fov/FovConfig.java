package com.legacyvisualfix.fov;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** Client configuration, independent of resource reload themes and tooltip animation. */
public final class FovConfig {

    public static boolean enabled = true;
    public static int transitionMs = 300;

    private FovConfig() {}

    public static void load(File configDirectory) {
        Configuration config = new Configuration(new File(configDirectory, "legacyvisualfix/fov.cfg"));
        config.load();
        enabled = config.getBoolean("enabled", "fov", true, "Smooth movement FOV transitions. Restart to apply.");
        transitionMs = config.getInt(
            "transitionMs",
            "fov",
            300,
            0,
            2000,
            "Time to cover 95% of a FOV multiplier change at 20 TPS. 0 restores vanilla smoothing. Restart to apply.");
        if (config.hasChanged()) config.save();
    }
}
