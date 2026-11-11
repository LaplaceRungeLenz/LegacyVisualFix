package com.legacyvisualfix.mixin.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.chat.ChatSession;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Inject(method = "addToSentMessages", at = @At("HEAD"))
    private void legacyvisualfix$storeCommand(String message, CallbackInfo ci) {
        ChatSession.record(Minecraft.getMinecraft(), message);
    }
}
