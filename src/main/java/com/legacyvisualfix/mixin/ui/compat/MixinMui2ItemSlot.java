package com.legacyvisualfix.mixin.ui.compat;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.legacyvisualfix.ui.UiEffects;

@Mixin(value = ItemSlot.class, remap = false)
public abstract class MixinMui2ItemSlot {

    @Inject(
        method = "drawSlot",
        at = @At(value = "INVOKE", target = "Lcom/cleanroommc/modularui/utils/Platform;setupDrawItem()V"),
        remap = false)
    private void legacyvisualfix$beginItem(ModularSlot slot, CallbackInfo ci) {
        ItemSlot widget = (ItemSlot) (Object) this;
        boolean eligible = widget.isEnabled() && slot.func_111238_b() && !slot.isPhantom();
        UiEffects.beginSlot(
            Minecraft.getMinecraft().currentScreen,
            eligible ? slot : null,
            9,
            9,
            widget.getContext()
                .getTopHovered() == widget);
    }

    // MUI2 may nest an NEA matrix. Restore only after it has restored its own matrix, before NEI overlays.
    @Inject(
        method = "drawSlot",
        at = @At(
            value = "INVOKE",
            target = "Lcom/cleanroommc/modularui/screen/NEAAnimationHandler;endHoverScale()V",
            shift = At.Shift.AFTER),
        remap = false)
    private void legacyvisualfix$endItem(ModularSlot slot, CallbackInfo ci) {
        UiEffects.endItem();
    }
}
