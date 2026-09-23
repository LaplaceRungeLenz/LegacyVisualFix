/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Adapted from PinkYuDeer/GTNH-Qol-Improvements. See META-INF/NOTICE-Vajra.md.
 */
package com.legacyvisualfix.vajra.client;

import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.legacyvisualfix.vajra.VajraBlockTarget;
import com.legacyvisualfix.vajra.VajraConfig;
import com.legacyvisualfix.vajra.VajraEventHandler;
import com.legacyvisualfix.vajra.VajraNetwork;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTECable;
import gregtech.client.BlockOverlayRenderer;

/** Connects the Vajra to GT's own 3x3 wrench/cutter target overlay. */
public final class VajraOverlayHandler {

    private static final Method DRAW_GRID = findDrawGrid();
    private boolean suppressFallbackAirUse;
    private ItemStack registeredFullBlockTool;
    private volatile boolean serverSupportsTools;
    private boolean ownsFullBlockRegistration;

    @SubscribeEvent
    public void onChannels(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (event.side.isClient() && event.registrations.contains("lvf_vajra"))
            serverSupportsTools = "REGISTER".equals(event.operation);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        serverSupportsTools = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        if (!VajraBlockTarget.isBlockHit(event.target) || !VajraEventHandler.isVajra(event.currentItem)) {
            return;
        }

        if (!VajraConfig.enabled || !serverSupportsTools) return;

        TileEntity tile = event.player.worldObj
            .getTileEntity(event.target.blockX, event.target.blockY, event.target.blockZ);
        Block block = event.player.worldObj.getBlock(event.target.blockX, event.target.blockY, event.target.blockZ);
        boolean cable = false;
        boolean wrenchTarget = VajraEventHandler.isWrenchTarget(
            tile,
            block,
            event.player.worldObj,
            event.target.blockX,
            event.target.blockY,
            event.target.blockZ);
        if (tile instanceof IGregTechTileEntity gtTile) {
            IMetaTileEntity meta = gtTile.getMetaTileEntity();
            cable = meta instanceof MTECable;
        }
        if (!wrenchTarget || DRAW_GRID == null) {
            return;
        }

        try {
            // Cables use cutter colours/semantics; every other supported target uses the
            // native wrench variant. The private renderer preserves GT's exact nine zones.
            DRAW_GRID.invoke(null, event, false, !cable, !cable && event.player.isSneaking());
        } catch (ReflectiveOperationException ignored) {}
    }

    /**
     * Minecraft 1.7.10 checks only cancellation on the client interaction event; its
     * useItem result is ignored there. Send the precise ray hit to our server handler, then
     * cancel the local path before ToolVajra.onItemUse can harvest the clicked block.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClick(PlayerInteractEvent event) {
        if (!event.world.isRemote || event.entityPlayer == null
            || !VajraEventHandler.isVajra(event.entityPlayer.getHeldItem())
            || !VajraConfig.enabled
            || !serverSupportsTools) {
            return;
        }
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR && suppressFallbackAirUse) {
            // Minecraft treats a canceled block click as unhandled and immediately attempts
            // RIGHT_CLICK_AIR. Suppress only that synthetic fallback; a genuine air click on
            // a later input remains the Vajra's silk-touch toggle.
            suppressFallbackAirUse = false;
            event.setCanceled(true);
            return;
        }
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (!VajraEventHandler.isWrenchTarget(
            event.world.getTileEntity(event.x, event.y, event.z),
            event.world.getBlock(event.x, event.y, event.z),
            event.world,
            event.x,
            event.y,
            event.z)) return;
        float hitX = 0.5F;
        float hitY = 0.5F;
        float hitZ = 0.5F;
        MovingObjectPosition target = minecraft.objectMouseOver;
        if (target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && target.blockX == event.x
            && target.blockY == event.y
            && target.blockZ == event.z) {
            Vec3 hit = target.hitVec;
            hitX = clamp((float) (hit.xCoord - event.x));
            hitY = clamp((float) (hit.yCoord - event.y));
            hitZ = clamp((float) (hit.zCoord - event.z));
        }
        VajraNetwork.vajraToolClick(event.x, event.y, event.z, event.face, hitX, hitY, hitZ);
        suppressFallbackAirUse = true;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) updateFullBlockRayTraceRegistration(Minecraft.getMinecraft());
        else suppressFallbackAirUse = false;
    }

    private void updateFullBlockRayTraceRegistration(Minecraft minecraft) {
        ItemStack held = minecraft != null && minecraft.thePlayer != null ? minecraft.thePlayer.getHeldItem() : null;
        boolean shouldRegister = VajraConfig.enabled && serverSupportsTools && VajraEventHandler.isVajra(held);
        if (registeredFullBlockTool != null && (!shouldRegister || registeredFullBlockTool.getItem() != held.getItem()
            || registeredFullBlockTool.getItemDamage() != held.getItemDamage())) {
            if (ownsFullBlockRegistration) VajraEventHandler.unregisterAsGtWireCutter(registeredFullBlockTool);
            registeredFullBlockTool = null;
        }
        if (shouldRegister && registeredFullBlockTool == null) {
            registeredFullBlockTool = held.copy();
            ownsFullBlockRegistration = VajraEventHandler.registerAsGtWireCutter(registeredFullBlockTool);
        }
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static Method findDrawGrid() {
        try {
            Method method = BlockOverlayRenderer.class.getDeclaredMethod(
                "drawGrid",
                DrawBlockHighlightEvent.class,
                boolean.class,
                boolean.class,
                boolean.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
