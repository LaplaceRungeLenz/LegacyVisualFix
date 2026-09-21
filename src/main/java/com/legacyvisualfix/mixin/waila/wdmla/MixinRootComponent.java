package com.legacyvisualfix.mixin.waila.wdmla;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.wdmla.impl.ui.component.RootComponent;
import com.gtnewhorizons.wdmla.impl.ui.sizer.Size;
import com.legacyvisualfix.waila.WdmlaAnimationRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = RootComponent.class, remap = false)
public abstract class MixinRootComponent {

    @WrapMethod(method = "renderHUD")
    private void legacyvisualfix$render(Operation<Void> original) {
        RootComponent root = (RootComponent) (Object) this;
        try (WdmlaAnimationRenderer.Frame frame = WdmlaAnimationRenderer.begin(root.getWidth(), root.getHeight())) {
            original.call();
        }
    }

    @ModifyArg(
        method = "renderHUD",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/wdmla/impl/ui/value/HUDRenderArea;<init>(Lcom/gtnewhorizons/wdmla/impl/ui/sizer/Size;)V"),
        index = 0)
    private Size legacyvisualfix$size(Size measured) {
        return WdmlaAnimationRenderer.size(measured);
    }

    @Inject(
        method = "renderHUD",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/wdmla/util/GLStateHelper;prepareFGDraw()V",
            shift = At.Shift.AFTER))
    private void legacyvisualfix$clip(CallbackInfo ci) {
        WdmlaAnimationRenderer.clipContent();
    }

    @Inject(
        method = "renderHUD",
        at = @At(value = "INVOKE", target = "Lcom/gtnewhorizons/wdmla/util/GLStateHelper;endDraw()V"))
    private void legacyvisualfix$unclip(CallbackInfo ci) {
        WdmlaAnimationRenderer.finishClip();
    }
}
