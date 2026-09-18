package com.legacyvisualfix.combat;

/** Shared presentation profile. Saved custom values and individual feature switches remain intact. */
public final class FeedbackStyle {

    private FeedbackStyle() {}

    private static float select(float custom, float light, float standard, float strong) {
        if ("light".equals(CombatConfig.feedbackPreset)) return light;
        if ("standard".equals(CombatConfig.feedbackPreset)) return standard;
        if ("strong".equals(CombatConfig.feedbackPreset)) return strong;
        return custom;
    }

    public static int particles() {
        return (int) select(CombatConfig.particlesPerHit, 3, 6, 8);
    }

    public static float particleScale() {
        float scale = select(CombatConfig.particleScale, 0.5F, 0.65F, 0.8F);
        return Float.isFinite(scale) ? Math.max(0.25F, Math.min(1F, scale)) : 0.65F;
    }

    public static float reactionDegrees() {
        return select(CombatConfig.reactionDegrees, 1.5F, 3F, 4F);
    }

    public static int reactionDuration() {
        return (int) select(CombatConfig.reactionDurationMs, 120, 140, 160);
    }

    public static float recoilDegrees() {
        return select(CombatConfig.recoilDegrees, 1.5F, 3F, 4F);
    }

    public static int recoilDuration() {
        return (int) select(CombatConfig.recoilDurationMs, 100, 120, 140);
    }

    public static int markerDuration() {
        return (int) select(CombatConfig.durationMs, 120, 160, 180);
    }

    public static float soundVolume() {
        return select(CombatConfig.soundVolume, 0.12F, 0.2F, 0.25F);
    }

    public static boolean excluded(String[] entries, String id, String className) {
        for (String entry : entries) {
            if (entry != null && (entry.trim()
                .equals(id)
                || entry.trim()
                    .equals(className)))
                return true;
        }
        return false;
    }
}
