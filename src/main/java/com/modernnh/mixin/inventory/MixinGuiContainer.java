package com.modernnh.mixin.inventory;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.modernnh.inventory.InventoryAnimationConfig;
import com.modernnh.inventory.InventoryAnimations;
import com.modernnh.inventory.InventoryMotion;
import com.modernnh.inventory.InventoryMotionAccess;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen implements InventoryMotionAccess {

    @Shadow
    protected int guiTop;

    @Shadow
    protected abstract void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y);

    @Shadow
    protected abstract void drawGuiContainerForegroundLayer(int x, int y);

    @Shadow
    private void func_146977_a(Slot slot) {}

    @Unique
    private final InventoryMotion modernnh$motion = new InventoryMotion();

    @Override
    public InventoryMotion modernnh$inventoryMotion() {
        return modernnh$motion;
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void modernnh$initialize(CallbackInfo ci) {
        if (InventoryAnimations.motion(this) != null) modernnh$motion.initialize(
            InventoryAnimationConfig.enabled,
            InventoryAnimationConfig.durationMs,
            InventoryAnimationConfig.distance);
    }

    // After NEI's preDraw/layout adjustment, before any animated component is rendered.
    @Inject(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerBackgroundLayer(FII)V"))
    private void modernnh$frame(int x, int y, float partialTicks, CallbackInfo ci) {
        if (InventoryAnimations.motion(this) != null) modernnh$motion.frame(System.nanoTime(), height, guiTop);
    }

    @Redirect(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerBackgroundLayer(FII)V"))
    private void modernnh$background(GuiContainer gui, float ticks, int x, int y) {
        InventoryAnimations.draw(this, () -> drawGuiContainerBackgroundLayer(ticks, x, y));
    }

    @Redirect(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/inventory/GuiContainer;func_146977_a(Lnet/minecraft/inventory/Slot;)V"))
    private void modernnh$slot(GuiContainer gui, Slot slot) {
        InventoryAnimations.draw(this, () -> func_146977_a(slot));
    }

    @Redirect(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerForegroundLayer(II)V"))
    private void modernnh$foreground(GuiContainer gui, int x, int y) {
        InventoryAnimations.draw(this, () -> drawGuiContainerForegroundLayer(x, y));
    }

    @Inject(method = "isMouseOverSlot", at = @At("HEAD"), cancellable = true)
    private void modernnh$hover(Slot slot, int x, int y, CallbackInfoReturnable<Boolean> cir) {
        if (InventoryAnimations.entering(this)) cir.setReturnValue(false);
    }

    @Inject(method = "func_146978_c", at = @At("HEAD"), cancellable = true)
    private void modernnh$region(int left, int top, int width, int height, int x, int y,
        CallbackInfoReturnable<Boolean> cir) {
        if (InventoryAnimations.entering(this)) cir.setReturnValue(false);
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void modernnh$close(CallbackInfo ci) {
        modernnh$motion.finish();
    }
}
