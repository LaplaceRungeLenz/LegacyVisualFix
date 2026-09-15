package com.legacyvisualfix.mixin.ui.compat;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.core.mixins.early.minecraft.GuiContainerAccessor;
import com.cleanroommc.modularui.screen.ClientScreenHandler;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.legacyvisualfix.ui.UiEffects;

/** MUI2 cancels Forge's draw event and renders here, bypassing GuiContainer.drawScreen. */
@Mixin(value = ClientScreenHandler.class, remap = false)
public abstract class MixinMui2ScreenHandler {

    @Inject(method = "drawContainer", at = @At("HEAD"), remap = false)
    private static void legacyvisualfix$frame(ModularScreen screen, GuiContainer gui, int x, int y, float partial,
        CallbackInfo ci) {
        UiEffects.frame(gui, x, y);
    }

    // Screen-space here: the carried-item GUI translation happens after the foreground stage.
    @Inject(
        method = "drawContainer",
        at = @At(value = "INVOKE", target = "Lcodechicken/nei/guihook/GuiContainerManager;renderToolTips(II)V"),
        remap = false)
    private static void legacyvisualfix$beforeNeiTooltip(ModularScreen screen, GuiContainer gui, int x, int y,
        float partial, CallbackInfo ci) {
        UiEffects.finishParticleLayer(gui);
    }

    @Inject(
        method = "drawContainer",
        at = @At(
            value = "INVOKE",
            target = "Lcom/cleanroommc/modularui/core/mixins/early/minecraft/GuiContainerAccessor;invokeDrawGuiContainerForegroundLayer(II)V"),
        remap = false)
    private static void legacyvisualfix$beforeForeground(ModularScreen screen, GuiContainer gui, int x, int y,
        float partial, CallbackInfo ci) {
        UiEffects.finishParticleLayer(gui);
    }

    @Inject(method = "drawContainer", at = @At("RETURN"), remap = false)
    private static void legacyvisualfix$particles(ModularScreen screen, GuiContainer gui, int x, int y, float partial,
        CallbackInfo ci) {
        UiEffects.renderParticles(gui);
    }

    @Inject(
        method = "drawItemStack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItemAndEffectIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;II)V",
            remap = true),
        remap = false)
    private static void legacyvisualfix$beginCarried(GuiContainer gui, ItemStack stack, int x, int y, String label,
        CallbackInfo ci) {
        UiEffects.beginCarried(gui, stack == ((GuiContainerAccessor) gui).getReturningStack() ? null : stack, x, y);
    }

    @Inject(method = "drawItemStack", at = @At("RETURN"), remap = false)
    private static void legacyvisualfix$endCarried(GuiContainer gui, ItemStack stack, int x, int y, String label,
        CallbackInfo ci) {
        UiEffects.endItem();
    }
}
