package com.legacyvisualfix.mixin.waila.wdmla;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizons.wdmla.overlay.WDMlaTickHandler;
import com.legacyvisualfix.waila.WdmlaAnimationRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = WDMlaTickHandler.class, remap = false)
public abstract class MixinWDMlaTickHandler {

    @WrapMethod(method = "overlayRender")
    private void legacyvisualfix$overlay(RenderGameOverlayEvent.Post event, Operation<Void> original) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || Minecraft.getMinecraft().currentScreen != null) {
            original.call(event);
            return;
        }
        WdmlaAnimationRenderer.beginFrame();
        try {
            original.call(event);
        } finally {
            WdmlaAnimationRenderer.endFrame();
        }
    }

    @WrapMethod(method = "screenRender")
    private void legacyvisualfix$preview(GuiScreenEvent.DrawScreenEvent.Post event, Operation<Void> original) {
        WdmlaAnimationRenderer.beginFrame();
        try {
            original.call(event);
        } finally {
            WdmlaAnimationRenderer.endFrame();
        }
    }
}
