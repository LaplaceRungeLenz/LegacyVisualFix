package com.modernnh.mixin.ui.compat;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.modernnh.ui.UiEffects;

import codechicken.nei.guihook.GuiContainerManager;

/** NEI's relocated inventory renderer only; catalog, bookmarks and recipe ingredients use drawItem. */
@Mixin(value = GuiContainerManager.class, remap = false)
public abstract class MixinNeiSlotItems {

    @Shadow
    public GuiContainer window;

    @Inject(method = "drawSlotItem", at = @At("HEAD"), remap = false)
    private void modernnh$beginItem(Slot slot, ItemStack stack, int x, int y, String label, CallbackInfo ci) {
        boolean eligible = slot != null && window != null
            && slot.func_111238_b()
            && window.inventorySlots.inventorySlots.contains(slot);
        UiEffects.beginSlot(
            window,
            eligible ? slot : null,
            x + 8,
            y + 8,
            eligible && GuiContainerManager.getSlotMouseOver(window) == slot);
    }

    @Inject(method = "drawSlotItem", at = @At("RETURN"), remap = false)
    private void modernnh$endItem(Slot slot, ItemStack stack, int x, int y, String label, CallbackInfo ci) {
        UiEffects.endItem();
    }
}
