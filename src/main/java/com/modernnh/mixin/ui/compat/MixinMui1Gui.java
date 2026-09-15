package com.modernnh.mixin.ui.compat;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.modernnh.ui.UiEffects;

@Mixin(value = ModularGui.class, remap = false)
public abstract class MixinMui1Gui {

    @Inject(method = "drawScreen", at = @At("HEAD"), remap = true)
    private void modernnh$frame(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        UiEffects.frame(this, mouseX, mouseY);
    }

    @Inject(
        method = "drawItemStack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItemAndEffectIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;II)V",
            remap = true),
        remap = false)
    private void modernnh$beginCarried(ItemStack stack, int x, int y, String label, CallbackInfo ci) {
        ModularGui gui = (ModularGui) (Object) this;
        UiEffects.beginCarried(
            this,
            stack == gui.getAccessor()
                .getReturningStack() ? null : stack,
            x,
            y);
    }

    @Inject(method = "drawItemStack", at = @At("RETURN"), remap = false)
    private void modernnh$endCarried(ItemStack stack, int x, int y, String label, CallbackInfo ci) {
        UiEffects.endItem();
    }

    // Both anchors precede foreground tooltips and the later GUI-local carried-item translation.
    // Seal the layer even when empty, so freshly emitted particles wait until the next frame.
    @Inject(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/guihook/GuiContainerManager;renderToolTips(II)V",
            remap = false),
        remap = true)
    private void modernnh$beforeNeiTooltip(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        UiEffects.finishParticleLayer(this);
    }

    @Inject(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderHelper;disableStandardItemLighting()V",
            remap = true),
        remap = true)
    private void modernnh$beforeForeground(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        UiEffects.finishParticleLayer(this);
    }

    @Inject(method = "drawScreen", at = @At("RETURN"), remap = true)
    private void modernnh$particles(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        UiEffects.renderParticles(this);
    }
}
