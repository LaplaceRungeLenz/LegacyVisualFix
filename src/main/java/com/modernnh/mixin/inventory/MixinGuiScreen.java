package com.modernnh.mixin.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.modernnh.inventory.InventoryAnimations;
import com.modernnh.inventory.InventoryMotion;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Redirect(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiButton;drawButton(Lnet/minecraft/client/Minecraft;II)V"))
    private void modernnh$button(GuiButton button, Minecraft mc, int x, int y) {
        boolean entering = InventoryAnimations.entering(this);
        InventoryAnimations
            .draw((GuiScreen) (Object) this, () -> button.drawButton(mc, entering ? -10000 : x, entering ? -10000 : y));
    }

    @Redirect(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiLabel;func_146159_a(Lnet/minecraft/client/Minecraft;II)V"))
    private void modernnh$label(GuiLabel label, Minecraft mc, int x, int y) {
        InventoryAnimations.draw((GuiScreen) (Object) this, () -> label.func_146159_a(mc, x, y));
    }

    // Preserve other mods' dispatchers (notably ModularUI's Pre/Post input events).
    @WrapOperation(
        method = "handleInput",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;handleMouseInput()V"))
    private void modernnh$mouse(GuiScreen screen, Operation<Void> original) {
        InventoryMotion motion = InventoryAnimations.motion(screen);
        if (motion == null
            || !motion.mouse(Mouse.getEventButton(), Mouse.getEventButtonState(), Mouse.getEventDWheel()))
            original.call(screen);
    }

    @WrapOperation(
        method = "handleInput",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;handleKeyboardInput()V"))
    private void modernnh$key(GuiScreen screen, Operation<Void> original) {
        InventoryMotion motion = InventoryAnimations.motion(screen);
        int key = Keyboard.getEventKey();
        boolean close = key == Keyboard.KEY_ESCAPE
            || key == Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode();
        boolean modifier = key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT
            || key == Keyboard.KEY_LCONTROL
            || key == Keyboard.KEY_RCONTROL
            || key == Keyboard.KEY_LMENU
            || key == Keyboard.KEY_RMENU;
        if (motion != null
            && motion.key(Keyboard.getEventKeyState(), key, Keyboard.getEventCharacter(), close || modifier)) return;
        original.call(screen);
    }
}
