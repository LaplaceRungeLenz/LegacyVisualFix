package com.legacyvisualfix.mixin.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.chat.ChatSession;
import com.legacyvisualfix.screenshot.ScreenshotClipboard;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {

    @Shadow
    protected GuiTextField inputField;

    @Unique
    private boolean legacyvisualfix$closed;

    @Unique
    private boolean legacyvisualfix$intentionalClose;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void legacyvisualfix$copyScreenshot(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (button == 0 && !isShiftKeyDown()
            && ScreenshotClipboard.handleClick(
                mc,
                mc.ingameGUI.getChatGUI()
                    .func_146236_a(Mouse.getX(), Mouse.getY()))) {
            ci.cancel();
        }
    }

    @WrapMethod(method = "initGui")
    private void legacyvisualfix$restoreChat(Operation<Void> original) {
        ChatSession.enter(mc);
        GuiTextField previous = legacyvisualfix$closed ? null : inputField;
        legacyvisualfix$closed = false;
        original.call();

        if (previous != null) {
            // Resizing reinitializes the same chat screen.
            inputField.setText(previous.getText());
            inputField.setCursorPosition(previous.getCursorPosition());
            inputField.setSelectionPos(previous.getSelectionEnd());
        } else {
            String prefilled = inputField.getText();
            if (prefilled.isEmpty() || prefilled.equals("/")) {
                String draft = ChatSession.takeDraft(prefilled.equals("/"));
                if (draft != null) inputField.setText(draft);
            }
        }
    }

    @WrapOperation(
        method = "keyTyped",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V"))
    private void legacyvisualfix$closeFromKeyboard(Minecraft client, GuiScreen next, Operation<Void> original) {
        // Vanilla reaches these calls when the player presses Escape or submits the input.
        legacyvisualfix$intentionalClose = true;
        try {
            original.call(client, next);
        } finally {
            legacyvisualfix$intentionalClose = false;
        }
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void legacyvisualfix$saveInterruptedDraft(CallbackInfo ci) {
        legacyvisualfix$closed = true;
        if (!legacyvisualfix$intentionalClose && inputField != null) ChatSession.saveDraft(mc, inputField.getText());
    }
}
