package com.modernnh.mixin.waila;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.modernnh.waila.WailaAnimationRenderer;

import mcp.mobius.waila.overlay.OverlayRenderer;
import mcp.mobius.waila.overlay.Tooltip;

@Mixin(value = OverlayRenderer.class, remap = false)
public abstract class MixinOverlayRenderer {

    @Unique
    private static int modernnh$drawDepth;

    @Unique
    private static int modernnh$overlayDepth;

    @WrapMethod(method = "renderOverlay")
    private static void modernnh$frame(Tooltip tooltip, Operation<Void> original) {
        boolean outer = ++modernnh$overlayDepth == 1;
        if (outer) WailaAnimationRenderer.beginFrame();
        try {
            if (tooltip != null) original.call(tooltip);
        } finally {
            if (outer) WailaAnimationRenderer.endFrame();
            modernnh$overlayDepth--;
        }
    }

    @WrapMethod(method = "doRenderOverlay")
    private static void modernnh$draw(Tooltip tooltip, Operation<Void> original) {
        try {
            if (++modernnh$drawDepth == 1) {
                try (WailaAnimationRenderer.Frame frame = WailaAnimationRenderer.begin(tooltip)) {
                    original.call(tooltip);
                }
            } else {
                original.call(tooltip);
            }
        } finally {
            modernnh$drawDepth--;
        }
    }

    @ModifyArgs(
        method = "doRenderOverlay",
        at = @At(value = "INVOKE", target = "Lmcp/mobius/waila/overlay/OverlayRenderer;drawTooltipBox(IIIIIII)V"))
    private static void modernnh$background(Args args) {
        WailaAnimationRenderer.Frame frame = WailaAnimationRenderer.current();
        if (frame != null && modernnh$drawDepth == 1) {
            args.set(2, frame.width);
            args.set(3, frame.height);
        }
    }

    @Inject(
        method = "doRenderOverlay",
        at = @At(value = "INVOKE", target = "Lmcp/mobius/waila/overlay/Tooltip;draw()V"))
    private static void modernnh$clip(Tooltip tooltip, CallbackInfo ci) {
        WailaAnimationRenderer.Frame frame = WailaAnimationRenderer.current();
        if (frame != null && modernnh$drawDepth == 1) frame.clipContent();
    }

    @Inject(
        method = "doRenderOverlay",
        at = @At(value = "INVOKE", target = "Lmcp/mobius/waila/overlay/OverlayRenderer;loadGLState()V"))
    private static void modernnh$finishClip(Tooltip tooltip, CallbackInfo ci) {
        WailaAnimationRenderer.Frame frame = WailaAnimationRenderer.current();
        if (frame != null && modernnh$drawDepth == 1) frame.finishClip();
    }
}
