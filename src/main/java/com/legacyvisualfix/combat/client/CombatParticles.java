package com.legacyvisualfix.combat.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.AxisAlignedBB;

import com.legacyvisualfix.combat.CombatConfig;
import com.legacyvisualfix.combat.FeedbackStyle;
import com.legacyvisualfix.combat.HitFeedbackMessage;
import com.legacyvisualfix.combat.ParticleBudget;

import cpw.mods.fml.common.Loader;

/** Client-thread presentation only; protocol v1 supplies a target, not an exact impact point. */
public final class CombatParticles {

    private final ParticleBudget budget = new ParticleBudget();
    private final Random random = new Random(0x4D4E484954L);
    private final List<ActiveParticle> active = new ArrayList<ActiveParticle>(32);

    private static final class ActiveParticle {

        final HitParticle particle;
        final long emittedAt;

        ActiveParticle(HitParticle particle, long emittedAt) {
            this.particle = particle;
            this.emittedAt = emittedAt;
        }
    }

    public void spawn(Minecraft mc, HitFeedbackMessage hit, long nowMs) {
        if (mc == null || mc.theWorld == null
            || mc.thePlayer == null
            || mc.effectRenderer == null
            || mc.gameSettings == null
            || hit == null
            || !hit.valid()
            || !CombatConfig.enabled
            || hit.dimension != mc.thePlayer.dimension) return;
        boolean efr = Loader.isModLoaded("etfuturum");
        if (!ParticleBudget.allowed(
            CombatConfig.particleMode,
            efr,
            efr && "auto".equals(CombatConfig.particleMode) ? efrDamageIndicator() : null)) return;
        Entity target = mc.theWorld.getEntityByID(hit.targetId);
        if (target == null || target.isInvisible()
            || !finite(target.posX, target.posY, target.posZ)
            || !finite(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
            || mc.thePlayer.getDistanceSqToEntity(target) > 256
            || !mc.thePlayer.canEntityBeSeen(target)) return;
        if (FeedbackStyle.excluded(
            CombatConfig.particleExcludedEntities,
            EntityList.getEntityString(target),
            target.getClass()
                .getName()))
            return;
        AxisAlignedBB box = target.boundingBox;
        if (box == null || !finite(box.minX, box.minY, box.minZ) || !finite(box.maxX, box.maxY, box.maxZ)) return;
        double width = box.maxX - box.minX, depth = box.maxZ - box.minZ, height = box.maxY - box.minY;
        if (!finite(width, depth, height) || width <= 0 || depth <= 0 || height <= 0) return;
        double cx = box.minX + width * 0.5, cz = box.minZ + depth * 0.5;
        double dx = mc.thePlayer.posX - cx, dz = mc.thePlayer.posZ - cz;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (!Double.isFinite(length) || length < 0.001) return;
        dx /= length;
        dz /= length;
        double tx = Math.abs(dx) < 0.00001 ? Double.POSITIVE_INFINITY : width * 0.5 / Math.abs(dx);
        double tz = Math.abs(dz) < 0.00001 ? Double.POSITIVE_INFINITY : depth * 0.5 / Math.abs(dz);
        double side = Math.min(tx, tz);
        // Models (notably cows) can protrude beyond their collision box.
        double surfaceOffset = Math.max(0.04, Math.min(0.25, Math.min(width, depth) * 0.4));
        double x = cx + dx * (side + surfaceOffset), z = cz + dz * (side + surfaceOffset);
        // A compact patch on the facing side, with height/scatter bounded even for giant mobs.
        double margin = Math.min(0.1, height * 0.25);
        double y = Math.max(
            box.minY + margin,
            Math.min(box.minY + Math.min(height - margin, 2.0), mc.thePlayer.posY + mc.thePlayer.getEyeHeight()));
        if (!finite(x, y, z) || mc.thePlayer.getDistanceSq(x, y, z) > 256) return;
        expireActive(nowMs);
        int count = budget.claim(
            Math.min(FeedbackStyle.particles(), 32 - active.size()),
            mc.gameSettings.particleSetting,
            hit.health,
            hit.absorbed,
            nowMs);
        double scatter = Math.min(0.22, Math.min(width, depth) * 0.2);
        float targetScale = (float) Math.min(1, Math.sqrt(Math.min(width, depth) / 0.6));
        for (int i = 0; i < count; i++) {
            double tangent = (random.nextDouble() - 0.5) * scatter * 2;
            double py = Math.max(
                box.minY + margin,
                Math.min(box.maxY - margin, y + (random.nextDouble() - 0.5) * Math.min(0.3, height * 0.3)));
            HitParticle particle = new HitParticle(
                mc.theWorld,
                x + (tx < tz ? 0 : tangent),
                py,
                z + (tx < tz ? tangent : 0),
                dx * 0.025 + (random.nextDouble() - 0.5) * 0.025,
                0.015 + random.nextDouble() * 0.02,
                dz * 0.025 + (random.nextDouble() - 0.5) * 0.025,
                hit.health == 0,
                targetScale);
            active.add(new ActiveParticle(particle, nowMs));
            mc.effectRenderer.addEffect(particle);
        }
    }

    public void reset() {
        for (ActiveParticle entry : active) entry.particle.setDead();
        active.clear();
        budget.reset();
    }

    private void expireActive(long nowMs) {
        for (Iterator<ActiveParticle> iterator = active.iterator(); iterator.hasNext();) {
            ActiveParticle entry = iterator.next();
            if (entry.particle.isDead || ParticleBudget.activeExpired(entry.emittedAt, nowMs)) {
                // Vanilla may evict a particle without setting isDead. Kill even a still-rendered mote
                // before releasing its slot, so low FPS cannot exceed the active cap either.
                entry.particle.setDead();
                iterator.remove();
            }
        }
    }

    public void tick(Minecraft mc, long now) {
        if (!CombatConfig.enabled || "off".equals(CombatConfig.particleMode)
            || mc.theWorld == null
            || mc.thePlayer == null
            || mc.gameSettings.particleSetting >= 2) {
            reset();
        } else {
            expireActive(now);
        }
    }

    private static Boolean efrDamageIndicator() {
        try {
            return Class.forName("ganymedes01.etfuturum.configuration.configs.ConfigWorld")
                .getField("enableDmgIndicator")
                .getBoolean(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            // Auto must never double EFR feedback when its current setting cannot be determined.
            return null;
        }
    }

    private static boolean finite(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }
}
