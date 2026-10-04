package com.legacyvisualfix.mixin.ui.compat;

import net.minecraft.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.legacyvisualfix.ui.UiEffects;
import com.llamalad7.mixinextras.injector.WrapWithCondition;

@Mixin(value = SlotWidget.class, remap = false)
public abstract class MixinMui1SlotWidget {

    @WrapWithCondition(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/modularui/common/internal/wrapper/ModularGui;drawSolidRect(FFFFI)V"),
        remap = false)
    private boolean legacyvisualfix$hoverOverlay(float x, float y, float width, float height, int color) {
        SlotWidget widget = (SlotWidget) (Object) this;
        return widget.isPhantom() || !UiEffects.hideHoverOverlay(
            widget.getContext()
                .getScreen(),
            widget.getMcSlot());
    }

    // Start outside MUI's own icon matrix, after the slot underlay. Item coordinates are widget-local.
    @Inject(
        method = "drawSlot(Lnet/minecraft/inventory/Slot;Z)V",
        at = @At(value = "INVOKE", target = "Lcom/gtnewhorizons/modularui/api/GlStateManager;enableRescaleNormal()V"),
        remap = false)
    private void legacyvisualfix$beginItem(Slot slot, boolean renderAmount, CallbackInfo ci) {
        SlotWidget widget = (SlotWidget) (Object) this;
        boolean eligible = widget.isEnabled() && widget.getMcSlot()
            .isEnabled() && !widget.isPhantom();
        UiEffects.beginSlot(
            widget.getContext()
                .getScreen(),
            eligible ? slot : null,
            9,
            9,
            widget.getContext()
                .getCursor()
                .getHovered() == widget);
    }

    // Both sites occur only in the non-null item branch; early drawSlot returns never enter a scope.
    @Inject(
        method = "drawSlot(Lnet/minecraft/inventory/Slot;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItemOverlayIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            shift = At.Shift.AFTER,
            remap = true),
        remap = false)
    private void legacyvisualfix$endItem(Slot slot, boolean renderAmount, CallbackInfo ci) {
        UiEffects.endItem();
    }
}
