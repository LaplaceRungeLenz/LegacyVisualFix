package com.legacyvisualfix.mixin.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.ui.UiEffectsConfig;
import com.legacyvisualfix.ui.UiMotion;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinHotbar {

    @Unique
    private long legacyvisualfix$last;
    @Unique
    private double legacyvisualfix$position;
    @Unique
    private Object legacyvisualfix$world;
    @Unique
    private boolean legacyvisualfix$pending;
    @Unique
    private int legacyvisualfix$x, legacyvisualfix$y, legacyvisualfix$u, legacyvisualfix$v, legacyvisualfix$width,
        legacyvisualfix$height;
    @Unique
    private static final ResourceLocation legacyvisualfix$widgets = new ResourceLocation("textures/gui/widgets.png");

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void legacyvisualfix$begin(int width, int height, float partialTicks, CallbackInfo ci) {
        legacyvisualfix$pending = false;
    }

    @WrapOperation(
        method = "renderHotbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/GuiIngameForge;drawTexturedModalRect(IIIIII)V",
            ordinal = 1,
            remap = true))
    private void legacyvisualfix$selector(GuiIngameForge gui, int x, int y, int u, int v, int width, int height,
        Operation<Void> original) {
        Minecraft mc = Minecraft.getMinecraft();
        int selected = mc.thePlayer.inventory.currentItem;
        long now = System.nanoTime();
        if (!UiEffectsConfig.enabled || !UiEffectsConfig.hotbar
            || legacyvisualfix$last == 0
            || legacyvisualfix$world != mc.theWorld) legacyvisualfix$position = selected;
        else legacyvisualfix$position = UiMotion
            .approach(legacyvisualfix$position, selected, UiEffectsConfig.speed, (now - legacyvisualfix$last) * 1e-9);
        legacyvisualfix$last = now;
        legacyvisualfix$world = mc.theWorld;
        if (!UiEffectsConfig.enabled || !UiEffectsConfig.hotbar) {
            original.call(gui, x, y, u, v, width, height);
            return;
        }
        legacyvisualfix$x = x + (int) Math.round((legacyvisualfix$position - selected) * 20);
        legacyvisualfix$y = y;
        legacyvisualfix$u = u;
        legacyvisualfix$v = v;
        legacyvisualfix$width = width;
        legacyvisualfix$height = height;
        legacyvisualfix$pending = true;
    }

    // After all item models/overlays, before Forge's HOTBAR Post event.
    @Inject(
        method = "renderHotbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderHelper;disableStandardItemLighting()V",
            shift = At.Shift.AFTER,
            remap = true))
    private void legacyvisualfix$foreground(int width, int height, float ticks, CallbackInfo ci) {
        if (!legacyvisualfix$pending) return;
        legacyvisualfix$pending = false;
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_TEXTURE_BIT);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1, 1, 1, 1);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(legacyvisualfix$widgets);
            ((GuiIngameForge) (Object) this).drawTexturedModalRect(
                legacyvisualfix$x,
                legacyvisualfix$y,
                legacyvisualfix$u,
                legacyvisualfix$v,
                legacyvisualfix$width,
                legacyvisualfix$height);
        } finally {
            GL11.glPopAttrib();
        }
    }
}
