package com.modernnh.vajra;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class VajraNetwork {

    public static final SimpleNetworkWrapper CHANNEL = new SimpleNetworkWrapper("modernnh_vajra");

    private VajraNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(VajraToolClickMessage.Handler.class, VajraToolClickMessage.class, 0, Side.SERVER);
        FMLCommonHandler.instance()
            .bus()
            .register(new ServerVajraClickQueue());
    }

    public static void vajraToolClick(int x, int y, int z, int face, float hitX, float hitY, float hitZ) {
        CHANNEL.sendToServer(new VajraToolClickMessage(x, y, z, face, hitX, hitY, hitZ));
    }
}
