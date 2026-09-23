package com.legacyvisualfix.vajra;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class VajraConfig {

    public static boolean enabled = true;

    private VajraConfig() {}

    public static void load(File directory) {
        Configuration config = new Configuration(new File(directory, "legacyvisualfix/vajra.cfg"));
        config.load();
        enabled = config.getBoolean(
            "enabled",
            "vajra",
            true,
            "Enable Vajra wrench/cutter grid interactions. Install on both sides. Restart to apply.");
        if (config.hasChanged()) config.save();
    }
}
