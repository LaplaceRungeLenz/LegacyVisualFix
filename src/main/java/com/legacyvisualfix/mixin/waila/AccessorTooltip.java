package com.legacyvisualfix.mixin.waila;

import java.awt.Point;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import mcp.mobius.waila.overlay.Tooltip;

@Mixin(value = Tooltip.class, remap = false)
public interface AccessorTooltip {

    @Accessor("x")
    int legacyvisualfix$getX();

    @Accessor("x")
    void legacyvisualfix$setX(int value);

    @Accessor("y")
    int legacyvisualfix$getY();

    @Accessor("y")
    void legacyvisualfix$setY(int value);

    @Accessor("w")
    int legacyvisualfix$getWidth();

    @Accessor("h")
    int legacyvisualfix$getHeight();

    @Accessor("pos")
    Point legacyvisualfix$getAnchor();
}
