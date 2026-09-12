package com.modernnh.mixin.angelica;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.modernnh.reload.ReloadScreen;

@Pseudo
@Mixin(targets = "net.coderbot.iris.gl.shader.GlShader", remap = false)
public abstract class MixinGlShader {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void modernnh$compiled(CallbackInfo ci) {
        ReloadScreen.shaderPulse();
    }
}
