package com.modernnh.mixin;

import net.minecraft.client.renderer.texture.TextureMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.modernnh.reload.ReloadScreen;

@Mixin(TextureMap.class)
public abstract class MixinTextureMap {

    // Optional because renderer replacements may replace the atlas method. Listener progress remains available.
    @Inject(
        method = "loadTextureAtlas",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/ProgressManager$ProgressBar;step(Ljava/lang/String;)V",
            shift = At.Shift.AFTER,
            remap = false),
        require = 0)
    private void modernnh$textureStage(CallbackInfo ci) {
        ReloadScreen.textureProgress();
    }

    @Inject(
        method = "loadTextureAtlas",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureUtil;uploadTextureMipmap([[IIIIIZZ)V",
            shift = At.Shift.AFTER),
        require = 0)
    private void modernnh$uploadProgress(CallbackInfo ci) {
        ReloadScreen.textureProgress();
    }
}
