package com.modernnh.mixin.waila.chromatic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.modernnh.waila.chromatic.ChromaticAnimation;

import mcp.mobius.waila.overlay.OverlayRenderer;
import mcp.mobius.waila.overlay.Tooltip;

@Mixin(value = OverlayRenderer.class, remap = false, priority = 900)
public abstract class MixinWailaOverlay {

    @Unique
    private static int modernnh$overlayDepth;

    @WrapMethod(method = "renderOverlay")
    private static void modernnh$chromaticFrame(Tooltip tooltip, Operation<Void> original) {
        boolean outer = ++modernnh$overlayDepth == 1;
        if (outer) ChromaticAnimation.beginOverlay(tooltip);
        try {
            original.call(tooltip);
        } finally {
            if (outer) ChromaticAnimation.endOverlay();
            modernnh$overlayDepth--;
        }
    }
}
