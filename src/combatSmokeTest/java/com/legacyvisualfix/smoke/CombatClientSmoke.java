package com.legacyvisualfix.smoke;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.combat.CombatConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class CombatClientSmoke {

    private int ticks;
    private boolean requested, complete;
    private boolean whiteSeen;

    public static void register() {
        CombatClientSmoke smoke = new CombatClientSmoke();
        FMLCommonHandler.instance()
            .bus()
            .register(smoke);
        MinecraftForge.EVENT_BUS.register(smoke);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (complete) {
            mc.loadWorld(null);
            mc.displayGuiScreen(null);
            mc.shutdown();
            return;
        }
        if (++ticks == 40) {
            CombatConfig.durationMs = 500;
            CombatConfig.debug = true;
            mc.gameSettings.hideGUI = false;
            mc.gameSettings.pauseOnLostFocus = false;
            mc.launchIntegratedServer(
                "lvf-combat-smoke-" + System.currentTimeMillis(),
                "Combat smoke",
                new WorldSettings(1, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (!requested && CombatSmoke.serverPassed && mc.thePlayer != null && ticks > 80) {
            mc.displayGuiScreen(null);
            requested = true;
            CombatSmoke.requestNetworkHit = true;
        }
        if (ticks > 1600) finish("FAIL: timed out waiting for confirmed marker", mc);
    }

    @SubscribeEvent
    public void pre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS || !requested || complete) return;
        int x = event.resolution.getScaledWidth() / 2, y = event.resolution.getScaledHeight() / 2;
        Gui.drawRect(x + 4, y + 4, x + 10, y + 10, 0xFF000000);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void post(RenderGameOverlayEvent.Post event) throws Exception {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS || !requested || complete) return;
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = event.resolution;
        int scale = res.getScaleFactor();
        int x = (res.getScaledWidth() / 2 + 5) * scale;
        int y = mc.displayHeight - (res.getScaledHeight() / 2 + 6) * scale;
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        int red = pixel.get(0) & 255, green = pixel.get(1) & 255, blue = pixel.get(2) & 255;
        if (!whiteSeen && red > 100 && green > 100 && blue > 100) {
            ScreenShotHelper.saveScreenshot(
                mc.mcDataDir,
                "combat-stage1-smoke.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            whiteSeen = true;
            CombatSmoke.networkAbsorption = true;
            CombatSmoke.requestNetworkHit = true;
        } else if (whiteSeen && red > 100 && green > red * 0.7 && blue < red * 0.6) {
            ScreenShotHelper.saveScreenshot(
                mc.mcDataDir,
                "combat-stage1-absorption-smoke.png",
                mc.displayWidth,
                mc.displayHeight,
                mc.getFramebuffer());
            finish(
                "PASS: integrated server checks, FML capability registration, S2C packet, main-thread feedback and rendered white/gold markers",
                mc);
        }
    }

    private void finish(String result, Minecraft mc) throws Exception {
        complete = true;
        Files.write(
            new File(mc.mcDataDir, "legacyvisualfix-combat-client-smoke.txt").toPath(),
            result.getBytes(StandardCharsets.UTF_8));
    }
}
