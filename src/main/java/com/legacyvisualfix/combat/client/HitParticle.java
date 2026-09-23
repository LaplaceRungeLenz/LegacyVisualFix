package com.legacyvisualfix.combat.client;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import com.legacyvisualfix.combat.CombatConfig;
import com.legacyvisualfix.combat.FeedbackStyle;

/** Tiny client-observed-hit mote using the vanilla generic sparkle atlas, never the critical-hit sprite. */
public final class HitParticle extends EntityFX {

    private final float initialScale;
    private final int minimumLight;

    public HitParticle(World world, double x, double y, double z, double vx, double vy, double vz,
        boolean absorptionOnly) {
        this(world, x, y, z, vx, vy, vz, absorptionOnly, 1F);
    }

    public HitParticle(World world, double x, double y, double z, double vx, double vy, double vz,
        boolean absorptionOnly, float targetScale) {
        super(world, x, y, z);
        // Spawn occurs after the vanilla particle update; the first render must not interpolate from zero.
        prevPosX = x;
        prevPosY = y;
        prevPosZ = z;
        motionX = vx;
        motionY = vy;
        motionZ = vz;
        particleMaxAge = 4;
        initialScale = FeedbackStyle.particleScale() * Math.max(0.55F, Math.min(1F, targetScale));
        particleScale = initialScale;
        minimumLight = Math.max(0, Math.min(15, CombatConfig.particleMinLight)) * 16;
        noClip = true;
        setParticleTextureIndex(164);
        if (absorptionOnly) setRBGColorF(1F, 0.75F, 0.2F);
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        int light = super.getBrightnessForRender(partialTick);
        // Preserve sky light and depth handling; only raise this particle's block-light channel.
        return (light & 0xFFFF0000) | Math.max(light & 0xFFFF, minimumLight);
    }

    @Override
    public void setDead() {
        super.setDead();
        particleAlpha = 0;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (++particleAge >= particleMaxAge) {
            setDead();
            return;
        }
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        motionX *= 0.7;
        motionY *= 0.7;
        motionZ *= 0.7;
        particleAlpha = 1F - particleAge / (float) particleMaxAge;
        particleScale = initialScale * particleAlpha;
    }
}
