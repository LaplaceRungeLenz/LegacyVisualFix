package com.modernnh.smoke;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import mcp.mobius.waila.overlay.RayTracing;

/** A scoped visible ray target; no connection, world ticks or user save are needed. */
final class SyntheticTarget implements AutoCloseable {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final WorldClient previousWorld = mc.theWorld;
    private final GuiScreen previousScreen = mc.currentScreen;
    private final Field targetField;
    private final Object previousTarget;

    static WorldClient createWorld() {
        Minecraft mc = Minecraft.getMinecraft();
        return new WorldClient(
            new NetHandlerPlayClient(mc, null, null),
            new WorldSettings(0, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT),
            0,
            EnumDifficulty.PEACEFUL,
            mc.mcProfiler);
    }

    SyntheticTarget(WorldClient world) throws Exception {
        targetField = RayTracing.class.getDeclaredField("target");
        targetField.setAccessible(true);
        previousTarget = targetField.get(RayTracing.instance());
        targetField.set(RayTracing.instance(), new MovingObjectPosition(new EntityPig(world)));
        mc.theWorld = world;
        mc.currentScreen = null;
    }

    @Override
    public void close() throws Exception {
        mc.theWorld = previousWorld;
        mc.currentScreen = previousScreen;
        targetField.set(RayTracing.instance(), previousTarget);
    }
}
