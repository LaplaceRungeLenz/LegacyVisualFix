package com.legacyvisualfix.combat.client;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.combat.CombatConfig;
import com.legacyvisualfix.combat.FeedbackStyle;
import com.legacyvisualfix.combat.HitFeedback;

/** Client-thread state keyed by entity identity, never writes gameplay or model fields. */
public final class CombatReactions {

    private static final int MAX_TARGETS = 128;
    private static final Map<EntityLivingBase, Pulse> pulses = new IdentityHashMap<EntityLivingBase, Pulse>();
    private static long frameTime;

    private CombatReactions() {}

    private static final class Pulse {

        final long started;
        final int duration;
        final float fromX, fromZ, peakX, peakZ;

        Pulse(long started, int duration, float fromX, float fromZ, float peakX, float peakZ) {
            this.started = started;
            this.duration = duration;
            this.fromX = fromX;
            this.fromZ = fromZ;
            this.peakX = peakX;
            this.peakZ = peakZ;
        }

        boolean expired(long now) {
            return now < started || now - started < 0 || now - started >= duration;
        }

        float component(long now, float from, float peak) {
            if (expired(now)) return 0;
            long age = now - started;
            // Fast 20 ms attack, smooth recovery. Retrigger from the visible pose, not zero.
            if (age < 20) return from + (peak - from) * smooth(age / 20F);
            return peak * (1F - smooth((age - 20) / (float) (duration - 20)));
        }

        private static float smooth(float t) {
            return t * t * (3F - 2F * t);
        }
    }

    public static void reset() {
        pulses.clear();
        frameTime = 0;
    }

    public static void frame(long now) {
        frameTime = now;
    }

    public static void tick(Minecraft mc, long now) {
        if (!CombatConfig.enabled || !CombatConfig.modelReaction || mc.theWorld == null || mc.thePlayer == null) {
            reset();
            return;
        }
        for (Iterator<Map.Entry<EntityLivingBase, Pulse>> it = pulses.entrySet()
            .iterator(); it.hasNext();) {
            Map.Entry<EntityLivingBase, Pulse> entry = it.next();
            if (entry.getValue()
                .expired(now) || !eligible(mc, entry.getKey())) it.remove();
        }
    }

    public static void accept(Minecraft mc, HitFeedback hit, long now) {
        if (!CombatConfig.enabled || !CombatConfig.modelReaction
            || mc.theWorld == null
            || mc.thePlayer == null
            || hit == null
            || !hit.valid()
            || hit.dimension != mc.thePlayer.dimension) return;
        Entity entity = mc.theWorld.getEntityByID(hit.targetId);
        if (!(entity instanceof EntityLivingBase)) return;
        EntityLivingBase target = (EntityLivingBase) entity;
        if (!eligible(mc, target) || !mc.thePlayer.canEntityBeSeen(target)) return;
        String id = EntityList.getEntityString(target);
        if (FeedbackStyle.excluded(
            CombatConfig.reactionExcludedEntities,
            id,
            target.getClass()
                .getName()))
            return;
        double dx = target.posX - mc.thePlayer.posX, dz = target.posZ - mc.thePlayer.posZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        float degrees = FeedbackStyle.reactionDegrees();
        if (!Double.isFinite(length) || length < 0.001 || !Float.isFinite(degrees) || degrees <= 0) return;
        degrees = Math.min(degrees, 4F);
        Pulse previous = pulses.remove(target);
        float fromX = previous == null ? 0 : previous.component(now, previous.fromX, previous.peakX);
        float fromZ = previous == null ? 0 : previous.component(now, previous.fromZ, previous.peakZ);
        if (pulses.size() >= MAX_TARGETS) {
            EntityLivingBase oldest = null;
            long earliest = Long.MAX_VALUE;
            for (Map.Entry<EntityLivingBase, Pulse> entry : pulses.entrySet()) {
                if (oldest == null || entry.getValue().started < earliest) {
                    oldest = entry.getKey();
                    earliest = entry.getValue().started;
                }
            }
            pulses.remove(oldest);
        }
        pulses.put(
            target,
            new Pulse(
                now,
                Math.max(80, Math.min(300, FeedbackStyle.reactionDuration())),
                fromX,
                fromZ,
                (float) (dz / length * degrees),
                (float) (-dx / length * degrees)));
    }

    private static boolean eligible(Minecraft mc, EntityLivingBase target) {
        return target.worldObj == mc.theWorld && mc.theWorld.getEntityByID(target.getEntityId()) == target
            && !(target instanceof EntityPlayer)
            && !(target instanceof IBossDisplayData)
            && !target.isDead
            && target.deathTime == 0
            && target.getHealth() > 0
            && !target.isInvisible()
            && !target.isRiding()
            && target.riddenByEntity == null
            && Float.isFinite(target.height)
            && target.height > 0
            && target.height <= 4
            && Float.isFinite(target.width)
            && target.width > 0
            && target.width <= 3
            && Double.isFinite(target.posX)
            && Double.isFinite(target.posY)
            && Double.isFinite(target.posZ)
            && mc.thePlayer.getDistanceSqToEntity(target) <= 256;
    }

    /** Called inside vanilla's model matrix, before body yaw; axes are in world space. */
    public static void apply(EntityLivingBase target) {
        if (!CombatConfig.enabled || !CombatConfig.modelReaction || pulses.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null || !eligible(mc, target)) return;
        Pulse pulse = pulses.get(target);
        if (pulse == null) return;
        long now = frameTime == 0 ? System.nanoTime() / 1_000_000 : frameTime;
        float x = pulse.component(now, pulse.fromX, pulse.peakX);
        float z = pulse.component(now, pulse.fromZ, pulse.peakZ);
        float angle = (float) Math.sqrt(x * x + z * z);
        if (angle > 0.001F && Float.isFinite(angle)) GL11.glRotatef(Math.min(4F, angle), x / angle, 0, z / angle);
    }
}
