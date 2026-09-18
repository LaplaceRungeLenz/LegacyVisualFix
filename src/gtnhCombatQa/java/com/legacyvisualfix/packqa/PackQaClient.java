package com.legacyvisualfix.packqa;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class PackQaClient {

    private String saved = "";

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new PackQaClient());
        MinecraftForge.EVENT_BUS.register(new ParticleQa());
    }

    private boolean pending() {
        return !PackQa.capture.isEmpty() && !saved.equals(PackQa.capture);
    }

    @SubscribeEvent
    public void pre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS || !pending()) return;
        int x = event.resolution.getScaledWidth() / 2, y = event.resolution.getScaledHeight() / 2;
        Gui.drawRect(x + 4, y + 4, x + 10, y + 10, 0xFF000000);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void post(RenderGameOverlayEvent.Post event) throws Exception {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS || !pending()) return;
        long age = System.nanoTime() - PackQa.captureAt;
        if (age < 100_000_000L) return;
        Minecraft mc = Minecraft.getMinecraft();
        int scale = event.resolution.getScaleFactor();
        int x = (event.resolution.getScaledWidth() / 2 + 5) * scale;
        int y = mc.displayHeight - (event.resolution.getScaledHeight() / 2 + 6) * scale;
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        int r = pixel.get(0) & 255, g = pixel.get(1) & 255, b = pixel.get(2) & 255;
        boolean pass = !PackQa.capture.equals("gold") ? r > 90 && g > 90 && b > 90 : r > 90 && g > r * .7 && b < r * .6;
        if (!pass && age < 450_000_000L) return;
        saved = PackQa.capture;
        ScreenShotHelper.saveScreenshot(
            mc.mcDataDir,
            "lvf-pack-qa-" + saved + ".png",
            mc.displayWidth,
            mc.displayHeight,
            mc.getFramebuffer());
        String line = saved + " "
            + (pass ? "PASS" : "FAIL")
            + " pixel="
            + r
            + ","
            + g
            + ","
            + b
            + " ageMs="
            + age / 1_000_000
            + "\n";
        Files.write(
            new File(mc.mcDataDir, "lvf-pack-qa-client.txt").toPath(),
            line.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
    }
}
