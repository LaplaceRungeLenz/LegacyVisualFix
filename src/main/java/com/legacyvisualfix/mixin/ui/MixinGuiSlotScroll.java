package com.legacyvisualfix.mixin.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiResourcePackList;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.util.MathHelper;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.ui.MenuScroll;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(GuiSlot.class)
public abstract class MixinGuiSlotScroll implements MenuScroll {

    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    public int left;

    @Shadow
    public int right;

    @Shadow
    public int top;

    @Shadow
    public int bottom;

    @Shadow
    @Final
    public int slotHeight;

    @Shadow
    private float amountScrolled;

    @Shadow
    public abstract int func_148135_f();

    @Unique
    private boolean legacyvisualfix$smoothWheel;

    @Unique
    private boolean legacyvisualfix$scrolling;

    @Unique
    private float legacyvisualfix$scrollTarget;

    @Unique
    private long legacyvisualfix$scrollTime;

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void legacyvisualfix$registerList(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        legacyvisualfix$smoothWheel = mc.currentScreen instanceof GuiLanguage
            || mc.currentScreen instanceof GuiScreenResourcePacks && (Object) this instanceof GuiResourcePackList;
        if (!legacyvisualfix$smoothWheel) return;
        ((MenuScroll.Host) mc.currentScreen).legacyvisualfix$registerScrollList(this);
        if (Mouse.isButtonDown(0)) legacyvisualfix$scrolling = false;
    }

    // GuiSlot forwards each drained event to GuiScreen.handleMouseInput. Route it there exactly once.
    @ModifyExpressionValue(
        method = "drawScreen",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
    private int legacyvisualfix$useRoutedWheel(int wheel) {
        return legacyvisualfix$smoothWheel ? 0 : wheel;
    }

    @Override
    public boolean legacyvisualfix$scrollAt(int mouseX, int mouseY, int wheel) {
        if (mouseX <= left || mouseX >= right || mouseY <= top || mouseY >= bottom) return false;
        if (!legacyvisualfix$scrolling) {
            legacyvisualfix$scrollTarget = amountScrolled;
            legacyvisualfix$scrollTime = System.nanoTime();
            legacyvisualfix$scrolling = true;
        }
        int step = slotHeight / 2;
        legacyvisualfix$scrollTarget = legacyvisualfix$clampScroll(
            legacyvisualfix$scrollTarget + (wheel > 0 ? -step : step));
        return true;
    }

    @Inject(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiSlot;bindAmountScrolled()V",
            shift = At.Shift.AFTER))
    private void legacyvisualfix$animateScroll(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!legacyvisualfix$smoothWheel || !legacyvisualfix$scrolling) return;
        legacyvisualfix$scrollTarget = legacyvisualfix$clampScroll(legacyvisualfix$scrollTarget);
        long now = System.nanoTime();
        float fraction = (float) -Math.expm1(-(now - legacyvisualfix$scrollTime) / 55_000_000.0);
        legacyvisualfix$scrollTime = now;
        amountScrolled += (legacyvisualfix$scrollTarget - amountScrolled) * fraction;
        if (Math.abs(legacyvisualfix$scrollTarget - amountScrolled) < 0.5F) {
            amountScrolled = legacyvisualfix$scrollTarget;
            legacyvisualfix$scrolling = false;
        }
    }

    @Unique
    private float legacyvisualfix$clampScroll(float position) {
        return MathHelper.clamp_float(position, 0.0F, Math.max(0, func_148135_f()));
    }
}
