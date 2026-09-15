package com.modernnh.inventory;

/** Render-clock animation and input latch, independent of Minecraft and OpenGL. */
public final class InventoryMotion {

    private boolean initialized;
    private boolean entranceAllowed = true;
    private boolean entering;
    private boolean started;
    private long start;
    private int durationMs;
    private int distance;
    private float offset;
    private int suppressedButtons;

    /** A fresh GUI can still be a return from another inventory page. */
    public void openingFrom(boolean world, InventoryMotion previous) {
        entranceAllowed = world || (previous != null && previous.entranceAllowed && !previous.initialized);
    }

    public void initialize(boolean enabled, int duration, int configuredDistance) {
        if (initialized) {
            finish();
            return;
        }
        initialized = true;
        durationMs = duration;
        distance = configuredDistance;
        entering = enabled && entranceAllowed && duration > 0;
    }

    public float frame(long now, int height, int top) {
        if (!entering) return offset = 0;
        if (!started) {
            started = true;
            start = now;
        }
        double p = Math.max(0, Math.min(1, (now - start) / (durationMs * 1_000_000.0)));
        if (p >= 1) {
            finish();
            return 0;
        }
        double remaining = 1 - p;
        offset = (float) ((distance > 0 ? distance : Math.max(0, height - top + 64)) * remaining
            * remaining
            * remaining);
        return offset;
    }

    public float offset() {
        return offset;
    }

    public boolean isEntering() {
        return entering;
    }

    public boolean blocksHeldMouse() {
        return entering || suppressedButtons != 0;
    }

    public void finish() {
        entering = false;
        offset = 0;
    }

    /** NEI also accepts IME characters without a physical key-down event. */
    public boolean key(boolean down, int key, char character, boolean bypass) {
        if (!entering || bypass || !(down || (key == 0 && Character.isDefined(character)))) return false;
        finish();
        return true;
    }

    /** Consume an initiating action AND its release; never turn movement into an action. */
    public boolean mouse(int button, boolean down, int wheel) {
        int bit = button >= 0 && button < 32 ? 1 << button : 0;
        if (suppressedButtons != 0) {
            if (down) suppressedButtons |= bit;
            else suppressedButtons &= ~bit;
            return true;
        }
        if (!entering) return false;
        if (bit == 0 && wheel == 0) return false;
        if (down) suppressedButtons |= bit;
        finish();
        return true;
    }
}
