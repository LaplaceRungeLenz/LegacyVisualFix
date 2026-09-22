package com.legacyvisualfix.mixin.waila.wdmla;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizons.wdmla.api.accessor.Accessor;
import com.gtnewhorizons.wdmla.impl.ui.component.RootComponent;
import com.gtnewhorizons.wdmla.overlay.WDMlaTickHandler;
import com.legacyvisualfix.waila.PendingTooltip;
import com.legacyvisualfix.waila.WailaAnimationConfig;
import com.legacyvisualfix.waila.WdmlaAnimationRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import cpw.mods.fml.common.gameevent.TickEvent;

@Mixin(value = WDMlaTickHandler.class, remap = false)
public abstract class MixinWDMlaTickHandler {

    @Shadow
    private static RootComponent mainHUD;

    @Unique
    private final PendingTooltip<RootComponent> legacyvisualfix$pending = new PendingTooltip<>();

    @Unique
    private boolean legacyvisualfix$waitingForData;

    @Unique
    private Object legacyvisualfix$world, legacyvisualfix$screen;

    @Unique
    private void legacyvisualfix$clearPending() {
        legacyvisualfix$pending.clear();
        legacyvisualfix$world = null;
        legacyvisualfix$screen = null;
    }

    @WrapMethod(method = "handle")
    private RootComponent legacyvisualfix$completeHud(Accessor accessor, Operation<RootComponent> original) {
        legacyvisualfix$waitingForData = false;
        try {
            RootComponent next = original.call(accessor);
            if (!WailaAnimationConfig.enabled || WailaAnimationConfig.durationMs <= 0) {
                legacyvisualfix$clearPending();
                return next;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (legacyvisualfix$world != mc.theWorld || legacyvisualfix$screen != mc.currentScreen) {
                legacyvisualfix$clearPending();
            }
            legacyvisualfix$world = mc.theWorld;
            legacyvisualfix$screen = mc.currentScreen;
            return legacyvisualfix$pending.resolve(next, legacyvisualfix$waitingForData, System.nanoTime());
        } catch (RuntimeException | Error failure) {
            legacyvisualfix$clearPending();
            throw failure;
        } finally {
            legacyvisualfix$waitingForData = false;
        }
    }

    @WrapOperation(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizons/wdmla/impl/ObjectDataCenter;getServerData()Lnet/minecraft/nbt/NBTTagCompound;"))
    private NBTTagCompound legacyvisualfix$awaitingData(Operation<NBTTagCompound> original) {
        NBTTagCompound data = original.call();
        // This invocation occurs only after shouldDisplay, server connection and data-request checks.
        legacyvisualfix$waitingForData = data == null;
        return data;
    }

    @WrapMethod(method = "tickClient")
    private void legacyvisualfix$invalidateHidden(TickEvent.ClientTickEvent event, Operation<Void> original) {
        try {
            original.call(event);
        } finally {
            // Early returns (no target, hidden HUD, disconnected world, etc.) bypass handle entirely.
            if (event.phase == TickEvent.Phase.END && mainHUD == null) legacyvisualfix$clearPending();
        }
    }

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
