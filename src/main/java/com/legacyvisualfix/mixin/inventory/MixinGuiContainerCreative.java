package com.legacyvisualfix.mixin.inventory;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.legacyvisualfix.inventory.InventoryAnimations;
import com.legacyvisualfix.inventory.InventoryMotion;

@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreative {

    @Redirect(
        method = "drawScreen",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;isButtonDown(I)Z", remap = false))
    private boolean legacyvisualfix$scroll(int button) {
        InventoryMotion motion = InventoryAnimations.motion(this);
        return (motion == null || !motion.blocksHeldMouse()) && Mouse.isButtonDown(button);
    }

    @Redirect(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
    private int legacyvisualfix$page(FontRenderer font, String text, int x, int y, int color) {
        InventoryAnimations.draw((GuiScreen) (Object) this, () -> font.drawString(text, x, y, color));
        return 0; // Forge's page-counter call discards the return value.
    }

    @Inject(method = "renderCreativeInventoryHoveringText", at = @At("HEAD"), cancellable = true)
    private void legacyvisualfix$tabTooltip(CreativeTabs tab, int x, int y, CallbackInfoReturnable<Boolean> cir) {
        if (InventoryAnimations.entering(this)) cir.setReturnValue(false);
    }
}
