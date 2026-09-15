package com.legacyvisualfix.mixin.waila.chromatic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.legacyvisualfix.waila.chromatic.ChromaticAnimation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import mcp.mobius.waila.overlay.OverlayRenderer;
import mcp.mobius.waila.overlay.Tooltip;

@Mixin(value = OverlayRenderer.class, remap = false, priority = 900)
public abstract class MixinWailaOverlay {

    @Unique
    private static int legacyvisualfix$overlayDepth;

    @WrapMethod(method = "renderOverlay")
    private static void legacyvisualfix$chromaticFrame(Tooltip tooltip, Operation<Void> original) {
        boolean outer = ++legacyvisualfix$overlayDepth == 1;
        if (outer) ChromaticAnimation.beginOverlay(tooltip);
        try {
            original.call(tooltip);
        } finally {
            if (outer) ChromaticAnimation.endOverlay();
            legacyvisualfix$overlayDepth--;
        }
    }
}
