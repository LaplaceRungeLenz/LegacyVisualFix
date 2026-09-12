package com.modernnh.reload;

/** Smoothstep interpolation; elapsed and duration use the same units. */
public final class Fade {

    private Fade() {}

    public static double in(double elapsed, double duration) {
        if (duration <= 0) return 1;
        double t = Math.max(0, Math.min(1, elapsed / duration));
        return t * t * (3 - 2 * t);
    }

    public static double out(double elapsed, double duration) {
        return 1 - in(elapsed, duration);
    }
}
