package com.legacyvisualfix.mixin;

import java.io.File;

import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.legacyvisualfix.screenshot.ScreenshotClipboard;

@Mixin(ScreenShotHelper.class)
public abstract class MixinScreenShotHelper {

    @Inject(
        method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/IChatComponent;",
        at = @At("RETURN"))
    private static void legacyvisualfix$addCopyAction(File gameDir, String name, int width, int height,
        Framebuffer framebuffer, CallbackInfoReturnable<IChatComponent> cir) {
        ScreenshotClipboard.addCopyAction(cir.getReturnValue());
    }
}
