package com.modernnh.fov;

/** Converts a 95%-settling time into the coefficient used by Minecraft's 20 Hz FOV filter. */
public final class FovTransition {

    private FovTransition() {}

    public static float coefficient(boolean enabled, int transitionMs, float original) {
        if (!enabled || transitionMs <= 0) return original;
        return (float) (1.0 - Math.pow(0.05, 50.0 / transitionMs));
    }
}
