package com.legacyvisualfix.waila;

/** Fits the complete foreground without stretching glyphs or revealing individual columns. */
public final class TooltipContentTransform {

    public final float x, y, scale;

    public TooltipContentTransform(float x, float y, float width, float height, float targetWidth, float targetHeight) {
        this.x = x;
        this.y = y;
        scale = Math.max(
            0,
            Math.min(
                1,
                Math.min(targetWidth > 0 ? width / targetWidth : 1, targetHeight > 0 ? height / targetHeight : 1)));
    }

    public int viewportX(int value, double pixels) {
        double pivot = x * pixels;
        return (int) Math.round(pivot + (value - pivot) * scale);
    }

    public int viewportY(int value, double pixels, int displayHeight) {
        double pivot = displayHeight - y * pixels;
        return (int) Math.round(pivot + (value - pivot) * scale);
    }

    public int viewportSize(int value) {
        return Math.max(0, Math.round(value * scale));
    }
}
