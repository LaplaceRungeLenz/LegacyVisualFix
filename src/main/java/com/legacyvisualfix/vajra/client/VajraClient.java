package com.legacyvisualfix.vajra.client;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;

public final class VajraClient {

    private VajraClient() {}

    public static void register() {
        VajraOverlayHandler handler = new VajraOverlayHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
    }
}
