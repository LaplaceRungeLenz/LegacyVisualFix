package com.legacyvisualfix.packqa;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityCow;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.combat.CombatConfig;
import com.legacyvisualfix.combat.HitFeedbackMessage;
import com.legacyvisualfix.combat.client.CombatParticles;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

/** Disposable integrated-client checks; never packaged in the release. */
public final class ParticleQa {

    private String saved = "";
    private boolean suite;
    private String visual = "";

    @SubscribeEvent
    public void beforeHud(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || PackQa.capture.isEmpty()
            || visual.equals(PackQa.capture)) return;
        long age = (System.nanoTime() - PackQa.captureAt) / 1_000_000;
        Minecraft mc = Minecraft.getMinecraft();
        if (age < 50 || age > 200 || particles(mc).isEmpty()) return;
        captureWorld(mc, PackQa.capture);
        visual = PackQa.capture;
    }

    private void captureWorld(Minecraft mc, String name) {
        final int width = mc.displayWidth, height = mc.displayHeight;
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        final byte[] rgba = new byte[buffer.capacity()];
        buffer.get(rgba);
        final File output = new File(mc.mcDataDir, "screenshots/lvf-impact-" + name + ".png");
        // Pixel read stays on GL thread; PNG encoding must not stall the feedback timer.
        Thread writer = new Thread(() -> {
            try {
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                int[] row = new int[width];
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int i = (y * width + x) * 4;
                        row[x] = ((rgba[i] & 255) << 16) | ((rgba[i + 1] & 255) << 8) | (rgba[i + 2] & 255);
                    }
                    image.setRGB(0, height - y - 1, width, 1, row, 0, width);
                }
                ImageIO.write(image, "png", output);
            } catch (Exception failure) {
                failure.printStackTrace();
            }
        }, "LVF-QA-PNG");
        writer.setDaemon(true);
        writer.start();
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) throws Exception {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS || PackQa.capture.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        long age = (System.nanoTime() - PackQa.captureAt) / 1_000_000;
        if (!saved.equals(PackQa.capture) && age >= 90 && age < 500) {
            List<EntityFX> particles = particles(mc);
            if (particles.isEmpty() && age < 180) return;
            boolean colors = !particles.isEmpty();
            for (EntityFX p : particles) {
                colors &= !PackQa.capture.equals("gold")
                    ? p.getRedColorF() > 0.9F && p.getGreenColorF() > 0.9F && p.getBlueColorF() > 0.9F
                    : p.getRedColorF() > 0.9F && p.getGreenColorF() > 0.65F && p.getBlueColorF() < 0.3F;
            }
            log(
                "network " + PackQa.capture,
                colors && particles.size() <= 8,
                "count=" + particles.size() + " ageMs=" + age);
            saved = PackQa.capture;
        }
        if (!suite && PackQa.capture.equals("gold") && age > 1000) {
            suite = true;
            log("expired", particles(mc).isEmpty(), "remaining=" + particles(mc).size());
            policies(mc);
        }
    }

    private void policies(Minecraft mc) throws Exception {
        Entity target = null;
        for (Object value : mc.theWorld.loadedEntityList) {
            if (value instanceof EntityCow && !((Entity) value).isDead) {
                target = (Entity) value;
                break;
            }
        }
        if (target == null) {
            log("policy fixture", false, "no loaded cow");
            return;
        }
        String mode = CombatConfig.particleMode;
        int setting = mc.gameSettings.particleSetting;
        int count = CombatConfig.particlesPerHit;
        boolean invisible = target.isInvisible();
        Field efr = Class.forName("ganymedes01.etfuturum.configuration.configs.ConfigWorld")
            .getField("enableDmgIndicator");
        boolean efrBefore = efr.getBoolean(null);
        try {
            CombatConfig.particlesPerHit = 6;
            HitFeedbackMessage hit = new HitFeedbackMessage(1, mc.thePlayer.dimension, target.getEntityId(), 4, 0);
            mc.gameSettings.particleSetting = 0;
            CombatConfig.particleMode = "off";
            checkSpawn(mc, hit, "mode off", 0);
            CombatConfig.particleMode = "always";
            mc.gameSettings.particleSetting = 2;
            checkSpawn(mc, hit, "minimal", 0);
            mc.gameSettings.particleSetting = 0;
            CombatConfig.particleMode = "auto";
            efr.setBoolean(null, true);
            checkSpawn(mc, hit, "EFR enabled auto", 0);
            CombatConfig.particleMode = "always";
            checkSpawn(mc, hit, "EFR enabled always", 6);
            efr.setBoolean(null, false);
            CombatConfig.particleMode = "auto";
            checkSpawn(mc, hit, "EFR disabled auto", 6);
            mc.gameSettings.particleSetting = 1;
            checkSpawn(mc, hit, "decreased", 3);
            mc.gameSettings.particleSetting = 0;
            target.setInvisible(true);
            checkSpawn(mc, hit, "invisible", 0);
            target.setInvisible(false);
            checkSpawn(mc, new HitFeedbackMessage(2, mc.thePlayer.dimension, Integer.MIN_VALUE, 4, 0), "missing", 0);
            checkSpawn(
                mc,
                new HitFeedbackMessage(3, mc.thePlayer.dimension, target.getEntityId(), Float.NaN, 0),
                "invalid",
                0);
            CombatParticles fx = new CombatParticles();
            for (int i = 0; i < 100; i++) fx.spawn(mc, hit, 1000);
            int total = particles(mc).size();
            log("burst cap", total > 0 && total <= 32, "count=" + total);
            clear(mc);
        } finally {
            CombatConfig.particleMode = mode;
            CombatConfig.particlesPerHit = count;
            mc.gameSettings.particleSetting = setting;
            target.setInvisible(invisible);
            efr.setBoolean(null, efrBefore);
            clear(mc);
        }
    }

    private void checkSpawn(Minecraft mc, HitFeedbackMessage hit, String name, int expected) throws Exception {
        new CombatParticles().spawn(mc, hit, System.nanoTime() / 1_000_000);
        int actual = particles(mc).size();
        log(name, actual == expected, "count=" + actual + " expected=" + expected);
        clear(mc);
    }

    private List[] layers(Minecraft mc) {
        return ReflectionHelper.getPrivateValue(EffectRenderer.class, mc.effectRenderer, "fxLayers", "field_78876_b");
    }

    private List<EntityFX> particles(Minecraft mc) {
        List<EntityFX> result = new ArrayList<>();
        for (List layer : layers(mc)) for (Object p : layer) {
            if (p.getClass()
                .getName()
                .equals("com.legacyvisualfix.combat.client.HitParticle")) result.add((EntityFX) p);
        }
        return result;
    }

    private void clear(Minecraft mc) {
        for (List layer : layers(mc)) layer.removeAll(particles(mc));
    }

    private void log(String name, boolean pass, String detail) throws Exception {
        Files.write(
            new File(Minecraft.getMinecraft().mcDataDir, "lvf-pack-qa-particles.txt").toPath(),
            ((pass ? "PASS " : "FAIL ") + name + " " + detail + "\n").getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
    }
}
