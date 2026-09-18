package com.legacyvisualfix.ui;

/** Registry names only: never infer voltage from translated or player-provided item names. */
final class VoltageTrailRules {

    private static final String[] TIERS = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV",
        "UIV", "UMV", "UXV", "MAX" };
    private static final String[] COMPONENTS = { "Electric_Motor", "Electric_Pump", "Conveyor_Module",
        "Electric_Piston", "Robot_Arm", "Emitter", "Sensor", "Field_Generator", "Casing" };
    private static final String[] CIRCUITS = { "Primitive", "Basic", "Good", "Advanced", "Data", "Elite", "Master",
        "Ultimate", "Superconductor", "Infinite", "Bio", "Optical", "Exotic", "Cosmic", "Transcendent" };

    private VoltageTrailRules() {}

    static int componentTier(String name) {
        // Historical GT registry names predate UHV/MAX renaming. Hulls use their actual MTE tier.
        if ("Casing_MAX".equals(name)) return 9;
        if ("Casing_MAXV".equals(name)) return 14;
        for (String family : COMPONENTS) {
            for (int tier = 0; tier < TIERS.length; tier++) {
                if (name.equals(family + "_" + TIERS[tier])) return tier;
            }
        }
        return -1;
    }

    static int circuitTier(String oreName) {
        for (int tier = 0; tier < CIRCUITS.length; tier++) {
            if (oreName.equals("circuit" + CIRCUITS[tier])) return tier;
        }
        return -1;
    }

    static int formatColor(String format) {
        if (format == null) return -1;
        for (int i = 0; i + 1 < format.length(); i++) {
            if (format.charAt(i) != '\u00a7') continue;
            int index = "0123456789abcdef".indexOf(Character.toLowerCase(format.charAt(++i)));
            if (index < 0) continue;
            int extra = (index >> 3 & 1) * 85;
            int r = (index >> 2 & 1) * 170 + extra;
            int g = (index >> 1 & 1) * 170 + extra;
            int b = (index & 1) * 170 + extra;
            if (index == 6) r += 85;
            return r << 16 | g << 8 | b;
        }
        return -1;
    }
}
