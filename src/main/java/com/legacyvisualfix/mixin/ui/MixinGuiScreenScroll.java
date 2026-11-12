package com.legacyvisualfix.mixin.ui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.ui.MenuScroll;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenScroll implements MenuScroll.Host {

    @Shadow
    public Minecraft mc;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Unique
    private final List<MenuScroll> legacyvisualfix$scrollLists = new ArrayList<>(2);

    @Override
    public void legacyvisualfix$registerScrollList(MenuScroll list) {
        if (!legacyvisualfix$scrollLists.contains(list)) legacyvisualfix$scrollLists.add(list);
    }

    @Inject(method = "setWorldAndResolution", at = @At("HEAD"))
    private void legacyvisualfix$clearScrollLists(Minecraft client, int width, int height, CallbackInfo ci) {
        legacyvisualfix$scrollLists.clear();
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"))
    private void legacyvisualfix$routeWheel(CallbackInfo ci) {
        if (legacyvisualfix$scrollLists.isEmpty() || mc.currentScreen != (Object) this
            || mc.gameSettings.touchscreen
            || Mouse.isButtonDown(0)) return;
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        for (MenuScroll list : legacyvisualfix$scrollLists) {
            if (list.legacyvisualfix$scrollAt(mouseX, mouseY, wheel)) break;
        }
    }
}
