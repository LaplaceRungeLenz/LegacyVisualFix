package com.legacyvisualfix.waila;

/** One overlay's size, independent of the short-lived Waila tooltip objects. */
public final class TooltipAnimation {

    private boolean initialized;
    private long started, lastUpdated;
    private double width, height, startWidth, startHeight, targetWidth, targetHeight;

    public void update(double nextWidth, double nextHeight, long now, int durationMs) {
        lastUpdated = now;
        if (!initialized || durationMs <= 0) {
            width = startWidth = targetWidth = nextWidth;
            height = startHeight = targetHeight = nextHeight;
            initialized = true;
            started = now;
            return;
        }
        double t = Math.max(0, Math.min(1, (now - started) / (durationMs * 1_000_000.0)));
        double eased = t * t * (3 - 2 * t);
        width = startWidth + (targetWidth - startWidth) * eased;
        height = startHeight + (targetHeight - startHeight) * eased;
        if (nextWidth != targetWidth || nextHeight != targetHeight) {
            startWidth = width;
            startHeight = height;
            targetWidth = nextWidth;
            targetHeight = nextHeight;
            started = now;
        }
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public void reset() {
        initialized = false;
    }

    /** Empty render frames must not move the deadline forward. */
    public boolean resetIfIdle(long now, int timeoutMs) {
        if (!initialized || now - lastUpdated < timeoutMs * 1_000_000L) return false;
        reset();
        return true;
    }
}
