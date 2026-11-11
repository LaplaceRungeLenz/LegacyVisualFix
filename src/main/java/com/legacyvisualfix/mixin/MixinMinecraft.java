package com.legacyvisualfix.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.chat.ChatSession;
import com.legacyvisualfix.reload.ReloadScreen;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Inject(
        method = "runGameLoop",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;func_147120_f()V"))
    private void legacyvisualfix$transition(CallbackInfo ci) {
        ReloadScreen.renderTransition();
    }

    @Inject(method = "startGame", at = @At("RETURN"))
    private void legacyvisualfix$ready(CallbackInfo ci) {
        ReloadScreen.initialize((Minecraft) (Object) this);
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void legacyvisualfix$clearDisconnectedChat(WorldClient world, String loadingMessage, CallbackInfo ci) {
        if (world == null) ChatSession.leave((Minecraft) (Object) this);
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void legacyvisualfix$close(CallbackInfo ci) {
        ReloadScreen.shutdown();
    }
}
