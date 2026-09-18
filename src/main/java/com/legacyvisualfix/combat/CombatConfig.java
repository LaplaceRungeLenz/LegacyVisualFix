package com.legacyvisualfix.combat;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class CombatConfig {

    public static boolean enabled = true;
    public static boolean marker = true;
    public static boolean debug = false;
    public static int durationMs = 160;
    public static String soundMode = "auto";
    public static float soundVolume = 0.2F;

    private CombatConfig() {}

    public static void load(File directory) {
        Configuration config = new Configuration(new File(directory, "legacyvisualfix/combat.cfg"));
        config.load();
        enabled = config.getBoolean(
            "enabled",
            "combat",
            true,
            "Enable confirmed melee feedback. Server and client need protocol v1. Restart to apply.");
        marker = config.getBoolean(
            "marker",
            "client",
            true,
            "Brief crosshair marker: white = health lost, gold = absorption only. No attack prediction.");
        debug = config.getBoolean(
            "debug",
            "client",
            false,
            "Show the last confirmed health/absorption loss in HP above the crosshair.");
        durationMs = config
            .getInt("durationMs", "client", 160, 60, 500, "Marker duration in milliseconds; does not limit attacks.");
        soundMode = config.getString(
            "soundMode",
            "client",
            "auto",
            "auto: no extra sound when Et Futurum is installed; always: confirmed hit chime; off: silent.",
            new String[] { "auto", "always", "off" });
        soundVolume = config.getFloat(
            "soundVolume",
            "client",
            0.2F,
            0,
            1,
            "Volume of the optional vanilla successful-hit chime. EFR audio is never modified.");
        if (config.hasChanged()) config.save();
    }
}
