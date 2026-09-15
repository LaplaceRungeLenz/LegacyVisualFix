package com.legacyvisualfix.mixin.ui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.ui.UiEffects;

@Mixin(GuiContainer.class)
public abstract class MixinGuiItems {

    @Shadow
    protected int guiLeft;
    @Shadow
    protected int guiTop;
    @Shadow
    private ItemStack returningStack;
    @Unique
    private int legacyvisualfix$mouseX, legacyvisualfix$mouseY;

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void legacyvisualfix$frame(int x, int y, float partial, CallbackInfo ci) {
        legacyvisualfix$mouseX = x;
        legacyvisualfix$mouseY = y;
        UiEffects.frame(this, x, y);
    }

    @Inject(method = "func_146977_a", at = @At("HEAD"))
    private void legacyvisualfix$slot(Slot slot, CallbackInfo ci) {
        int x = legacyvisualfix$mouseX - guiLeft, y = legacyvisualfix$mouseY - guiTop;
        boolean hovered = slot.func_111238_b() && x >= slot.xDisplayPosition
            && x < slot.xDisplayPosition + 16
            && y >= slot.yDisplayPosition
            && y < slot.yDisplayPosition + 16;
        UiEffects.beginSlot(this, slot, slot.xDisplayPosition + 8, slot.yDisplayPosition + 8, hovered);
    }

    @Inject(method = "func_146977_a", at = @At("RETURN"))
    private void legacyvisualfix$slotEnd(Slot slot, CallbackInfo ci) {
        UiEffects.endItem();
    }

    @Inject(method = "drawItemStack", at = @At("HEAD"))
    private void legacyvisualfix$carried(ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        UiEffects.beginCarried(this, stack == returningStack ? null : stack, x, y);
    }

    @Inject(method = "drawItemStack", at = @At("RETURN"))
    private void legacyvisualfix$carriedEnd(ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        UiEffects.endItem();
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void legacyvisualfix$particles(int x, int y, float partial, CallbackInfo ci) {
        UiEffects.renderParticles(this);
    }

    @Inject(
        method = "drawScreen",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopMatrix()V", shift = At.Shift.AFTER),
        require = 0)
    private void legacyvisualfix$beforeTooltip(int x, int y, float partial, CallbackInfo ci) {
        UiEffects.renderParticles(this);
    }
}
