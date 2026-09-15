package com.modernnh.mixin.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.modernnh.ui.UiEffectsConfig;
import com.modernnh.ui.UiMotion;

@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinHotbar {

    @Unique
    private long modernnh$last;
    @Unique
    private double modernnh$position;
    @Unique
    private Object modernnh$world;
    @Unique
    private boolean modernnh$pending;
    @Unique
    private int modernnh$x, modernnh$y, modernnh$u, modernnh$v, modernnh$width, modernnh$height;
    @Unique
    private static final ResourceLocation modernnh$widgets = new ResourceLocation("textures/gui/widgets.png");

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void modernnh$begin(int width, int height, float partialTicks, CallbackInfo ci) {
        modernnh$pending = false;
    }

    @WrapOperation(
        method = "renderHotbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/GuiIngameForge;drawTexturedModalRect(IIIIII)V",
            ordinal = 1,
            remap = true))
    private void modernnh$selector(GuiIngameForge gui, int x, int y, int u, int v, int width, int height,
        Operation<Void> original) {
        Minecraft mc = Minecraft.getMinecraft();
        int selected = mc.thePlayer.inventory.currentItem;
        long now = System.nanoTime();
        if (!UiEffectsConfig.enabled || !UiEffectsConfig.hotbar || modernnh$last == 0 || modernnh$world != mc.theWorld)
            modernnh$position = selected;
        else modernnh$position = UiMotion
            .approach(modernnh$position, selected, UiEffectsConfig.speed, (now - modernnh$last) * 1e-9);
        modernnh$last = now;
        modernnh$world = mc.theWorld;
        if (!UiEffectsConfig.enabled || !UiEffectsConfig.hotbar) {
            original.call(gui, x, y, u, v, width, height);
            return;
        }
        modernnh$x = x + (int) Math.round((modernnh$position - selected) * 20);
        modernnh$y = y;
        modernnh$u = u;
        modernnh$v = v;
        modernnh$width = width;
        modernnh$height = height;
        modernnh$pending = true;
    }

    // After all item models/overlays, before Forge's HOTBAR Post event.
    @Inject(
        method = "renderHotbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderHelper;disableStandardItemLighting()V",
            shift = At.Shift.AFTER,
            remap = true))
    private void modernnh$foreground(int width, int height, float ticks, CallbackInfo ci) {
        if (!modernnh$pending) return;
        modernnh$pending = false;
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
                .bindTexture(modernnh$widgets);
            ((GuiIngameForge) (Object) this)
                .drawTexturedModalRect(modernnh$x, modernnh$y, modernnh$u, modernnh$v, modernnh$width, modernnh$height);
        } finally {
            GL11.glPopAttrib();
        }
    }
}
