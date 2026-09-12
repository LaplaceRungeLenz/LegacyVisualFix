package com.modernnh.mixin;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.modernnh.reload.ReloadScreen;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Inject(method = "startGame", at = @At("RETURN"))
    private void modernnh$ready(CallbackInfo ci) {
        ReloadScreen.initialize((Minecraft) (Object) this);
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void modernnh$close(CallbackInfo ci) {
        ReloadScreen.shutdown();
    }
}
