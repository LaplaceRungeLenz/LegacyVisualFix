package com.modernnh.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Observe navigation without replacing screens or depending on companion mods. */
public final class InventoryScreenEvents {

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new InventoryScreenEvents());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void opening(GuiOpenEvent event) {
        InventoryMotion next = InventoryAnimations.motion(event.gui);
        if (next == null) return;
        GuiScreen previous = Minecraft.getMinecraft().currentScreen;
        next.openingFrom(previous == null, InventoryAnimations.motion(previous));
    }
}
