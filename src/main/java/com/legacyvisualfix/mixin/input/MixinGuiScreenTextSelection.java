package com.legacyvisualfix.mixin.input;

import java.util.function.IntConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.input.TextSelectionScreen;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenTextSelection implements TextSelectionScreen {

    @Shadow
    public Minecraft mc;

    @Shadow
    public int width;

    @Unique
    private IntConsumer legacyvisualfix$textDrag;

    @Override
    public void legacyvisualfix$startTextSelection(IntConsumer drag) {
        legacyvisualfix$textDrag = drag;
    }

    @Inject(method = "setWorldAndResolution", at = @At("HEAD"))
    private void legacyvisualfix$resetTextDrag(Minecraft client, int width, int height, CallbackInfo ci) {
        legacyvisualfix$textDrag = null;
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"))
    private void legacyvisualfix$dragText(CallbackInfo ci) {
        if (legacyvisualfix$textDrag == null) return;
        int button = Mouse.getEventButton();
        boolean moving = button == -1 && Mouse.getEventDWheel() == 0 && Mouse.isButtonDown(0);
        boolean releasing = button == 0 && !Mouse.getEventButtonState();
        if (moving || releasing) legacyvisualfix$textDrag.accept(Mouse.getEventX() * width / mc.displayWidth);
        if (button == 0) legacyvisualfix$textDrag = null;
    }
}
