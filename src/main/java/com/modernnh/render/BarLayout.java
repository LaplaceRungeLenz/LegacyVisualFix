package com.modernnh.render;

/** GUI-scale coordinates, clamped so theme settings cannot move the bar off screen. */
public final class BarLayout {

    public final double x;
    public final double y;
    public final double width;
    public final double height;

    public BarLayout(int screenWidth, int screenHeight, int desiredWidth, int desiredHeight, double anchorX,
        double anchorY) {
        width = Math.min(desiredWidth, Math.max(1, screenWidth - 16));
        height = Math.min(desiredHeight, Math.max(1, screenHeight - 8));
        x = Math.max(0, Math.min(screenWidth - width, screenWidth * anchorX - width / 2));
        y = Math.max(0, Math.min(screenHeight - height, screenHeight * anchorY - height / 2));
    }

    public double filledWidth(double fraction) {
        return width * Math.max(0, Math.min(1, fraction));
    }
}
