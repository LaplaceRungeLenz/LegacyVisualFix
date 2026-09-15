package com.legacyvisualfix.mixin.waila.chromatic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.legacyvisualfix.waila.chromatic.ChromaticAnimation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.slprime.chromatictooltips.TooltipHandler;
import com.slprime.chromatictooltips.api.ITooltipRenderer;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.api.TooltipRequest;

@Mixin(value = TooltipContext.class, remap = false)
public abstract class MixinTooltipContext {

    @WrapMethod(method = "drawAt")
    private void legacyvisualfix$animate(int x, int y, Operation<Void> original) {
        try (ChromaticAnimation.Frame frame = ChromaticAnimation.begin((TooltipContext) (Object) this, x, y)) {
            original.call(frame == null ? x : frame.x, frame == null ? y : frame.y);
        }
    }

    // Compat copies the old renderer across Tooltip instances. Resolve the current theme so
    // resource reloads take effect even when the player keeps looking at the same block.
    @ModifyVariable(
        method = "<init>(Lcom/slprime/chromatictooltips/api/TooltipRequest;Lcom/slprime/chromatictooltips/api/ITooltipRenderer;)V",
        at = @At("HEAD"),
        argsOnly = true)
    private static ITooltipRenderer legacyvisualfix$currentTheme(ITooltipRenderer value, TooltipRequest request,
        ITooltipRenderer ignored) {
        return "waila".equals(request.context) ? TooltipHandler.getRendererFor(request) : value;
    }
}
