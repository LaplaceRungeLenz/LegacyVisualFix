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
    public static String particleMode = "auto";
    public static int particlesPerHit = 6;
    public static boolean modelReaction = true;
    public static float reactionDegrees = 3F;
    public static int reactionDurationMs = 140;
    public static String[] reactionExcludedEntities = new String[0];

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
        particleMode = config.getString(
            "particleMode",
            "client",
            "auto",
            "auto: confirmed local impact particles unless EFR damage particles are enabled or unknown; always: allow both; off: none. Respects Minimal particles.",
            new String[] { "auto", "always", "off" });
        particlesPerHit = config.getInt(
            "particlesPerHit",
            "client",
            6,
            1,
            8,
            "Maximum particles per confirmed melee result. Global burst budget applies; never affects damage.");
        modelReaction = config.getBoolean(
            "modelReaction",
            "client",
            true,
            "Small server-confirmed visual tilt. Never changes hitboxes, motion, AI or attack timing.");
        reactionDegrees = config.getFloat(
            "reactionDegrees",
            "client",
            3F,
            0F,
            4F,
            "Maximum visual tilt in degrees; repeated hits do not stack beyond this angle.");
        reactionDurationMs = config.getInt(
            "reactionDurationMs",
            "client",
            140,
            80,
            300,
            "Visual reaction duration in milliseconds. Independent of hurtTime and invulnerability.");
        reactionExcludedEntities = config.getStringList(
            "reactionExcludedEntities",
            "client",
            new String[0],
            "Skip model reaction for exact entity registry IDs or full Java class names. Players, bosses, mounts and oversized entities are always skipped.");
        if (config.hasChanged()) config.save();
    }
}
