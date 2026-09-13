package com.modernnh.mixin.waila;

import java.awt.Point;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import mcp.mobius.waila.overlay.Tooltip;

@Mixin(value = Tooltip.class, remap = false)
public interface AccessorTooltip {

    @Accessor("x")
    int modernnh$getX();

    @Accessor("x")
    void modernnh$setX(int value);

    @Accessor("y")
    int modernnh$getY();

    @Accessor("y")
    void modernnh$setY(int value);

    @Accessor("w")
    int modernnh$getWidth();

    @Accessor("h")
    int modernnh$getHeight();

    @Accessor("pos")
    Point modernnh$getAnchor();
}
