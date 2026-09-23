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
    public static boolean weaponRecoil = true;
    public static float recoilDegrees = 3F;
    public static int recoilDurationMs = 120;
    public static String feedbackPreset = "custom";
    public static int particleMinLight = 6;
    public static float particleScale = 0.65F;
    public static String[] particleExcludedEntities = new String[0];
    public static String[] recoilExcludedItems = new String[0];

    private CombatConfig() {}

    public static void load(File directory) {
        Configuration config = new Configuration(new File(directory, "legacyvisualfix/combat.cfg"));
        config.load();
        enabled = config.getBoolean(
            "enabled",
            "combat",
            true,
            "Enable client-only melee feedback from hurt signals after local attacks. Restart to apply.");
        marker = config.getBoolean(
            "marker",
            "client",
            true,
            "Brief white marker for client-observed hurt after a local attack; damage ownership is approximate.");
        debug = config.getBoolean(
            "debug",
            "client",
            false,
            "Label the client-observed hit above the crosshair; no precise damage or absorption amount.");
        durationMs = config
            .getInt("durationMs", "client", 160, 60, 500, "Marker duration in milliseconds; does not limit attacks.");
        soundMode = config.getString(
            "soundMode",
            "client",
            "auto",
            "auto: no extra sound when Et Futurum is installed; always: client-observed hit chime; off: silent.",
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
            "auto: client-observed local impact particles unless EFR damage particles are enabled or unknown; always: allow both; off: none. Respects Minimal particles.",
            new String[] { "auto", "always", "off" });
        particlesPerHit = config.getInt(
            "particlesPerHit",
            "client",
            6,
            1,
            8,
            "Maximum particles per client-observed melee result. Global burst budget applies; never affects damage.");
        modelReaction = config.getBoolean(
            "modelReaction",
            "client",
            true,
            "Small client-observed visual tilt. Never changes hitboxes, motion, AI or attack timing.");
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
        weaponRecoil = config.getBoolean(
            "weaponRecoil",
            "client",
            true,
            "Brief first-person held-item recoil after client-observed local melee. Never changes swing timing or input.");
        recoilDegrees = config.getFloat(
            "recoilDegrees",
            "client",
            3F,
            0F,
            5F,
            "Maximum held-item recoil angle. Repeated hits refresh rather than stack the effect.");
        recoilDurationMs = config.getInt(
            "recoilDurationMs",
            "client",
            120,
            60,
            240,
            "Visual recoil duration in milliseconds. Does not delay the next attack.");
        feedbackPreset = config.getString(
            "feedbackPreset",
            "client",
            "custom",
            "custom preserves individual settings; light/standard/strong choose coordinated intensity without rewriting them. Feature toggles and minimum light always apply.",
            new String[] { "custom", "light", "standard", "strong" });
        particleMinLight = config.getInt(
            "particleMinLight",
            "client",
            6,
            0,
            15,
            "Minimum block-light level for hit particles only. 0 follows scene lighting; never changes world lighting or depth testing. Shader appearance may vary.");
        particleScale = config.getFloat(
            "particleScale",
            "client",
            0.65F,
            0.25F,
            1F,
            "Base hit-particle scale in custom preset; additionally reduced for small targets.");
        particleExcludedEntities = config.getStringList(
            "particleExcludedEntities",
            "client",
            new String[0],
            "Disable local hit particles for exact entity registry IDs or full Java class names.");
        recoilExcludedItems = config.getStringList(
            "recoilExcludedItems",
            "client",
            new String[0],
            "Disable recoil for exact item registry IDs or full Java class names. Optional @metadata suffix selects one subtype (for example gregtech:gt.metatool.01@34).");
        if (config.hasChanged()) config.save();
    }
}
