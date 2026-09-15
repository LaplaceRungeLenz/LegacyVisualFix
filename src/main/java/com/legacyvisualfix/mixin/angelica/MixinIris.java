package com.legacyvisualfix.mixin.angelica;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.reload.ReloadScreen;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Pseudo
@Mixin(targets = "net.coderbot.iris.Iris", remap = false)
public abstract class MixinIris {

    @WrapMethod(method = "reload", remap = false)
    private static void legacyvisualfix$reload(Operation<Void> original) {
        ReloadScreen.runShaderReload(() -> original.call());
    }

    @Inject(
        method = "reload",
        at = @At(value = "INVOKE", target = "Lnet/coderbot/iris/Iris;destroyEverything()V", shift = At.Shift.AFTER),
        remap = false)
    private static void legacyvisualfix$destroyed(CallbackInfo ci) {
        ReloadScreen.shaderStep("Reading shader pack");
    }

    @Inject(
        method = "reload",
        at = @At(value = "INVOKE", target = "Lnet/coderbot/iris/Iris;loadShaderpack()V", shift = At.Shift.AFTER),
        remap = false)
    private static void legacyvisualfix$loaded(CallbackInfo ci) {
        ReloadScreen.shaderStep("Building rendering pipeline");
    }
}
