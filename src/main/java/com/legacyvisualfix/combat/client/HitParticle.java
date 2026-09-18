package com.legacyvisualfix.combat.client;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

/** Tiny confirmed-hit mote using the vanilla generic sparkle atlas, never the critical-hit sprite. */
public final class HitParticle extends EntityFX {

    public HitParticle(World world, double x, double y, double z, double vx, double vy, double vz,
        boolean absorptionOnly) {
        super(world, x, y, z);
        // Spawn occurs after the vanilla particle update; the first render must not interpolate from zero.
        prevPosX = x;
        prevPosY = y;
        prevPosZ = z;
        motionX = vx;
        motionY = vy;
        motionZ = vz;
        particleMaxAge = 4;
        particleScale = 0.65F;
        noClip = true;
        setParticleTextureIndex(164);
        if (absorptionOnly) setRBGColorF(1F, 0.75F, 0.2F);
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
        particleScale = 0.65F * particleAlpha;
    }
}
