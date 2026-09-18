package com.legacyvisualfix.combat.client;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

public class HitParticleTest {

    @Test
    public void firstFrameInterpolatesAtTheImpactBeforeAnyParticleTick() {
        HitParticle particle = new HitParticle(null, 100, 240, 200, 0.01, 0.02, 0.03, false);
        assertEquals(100, particle.prevPosX, 0);
        assertEquals(240, particle.prevPosY, 0);
        assertEquals(200, particle.prevPosZ, 0);
        assertEquals(particle.posX, particle.prevPosX, 0);
        assertEquals(particle.posY, particle.prevPosY, 0);
        assertEquals(particle.posZ, particle.prevPosZ, 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rendererEvictionWithoutDeathCannotPermanentlyConsumeActiveSlots() throws Exception {
        CombatParticles particles = new CombatParticles();
        Field activeField = CombatParticles.class.getDeclaredField("active");
        activeField.setAccessible(true);
        List<Object> active = (List<Object>) activeField.get(particles);
        Class<?> entryType = Class.forName(CombatParticles.class.getName() + "$ActiveParticle");
        Constructor<?> constructor = entryType.getDeclaredConstructor(HitParticle.class, long.class);
        constructor.setAccessible(true);
        HitParticle[] evicted = new HitParticle[32];
        for (int i = 0; i < evicted.length; i++) {
            evicted[i] = new HitParticle(null, 100, 240, 200, 0, 0, 0, false);
            active.add(constructor.newInstance(evicted[i], 1000L));
        }
        Method expire = CombatParticles.class.getDeclaredMethod("expireActive", long.class);
        expire.setAccessible(true);
        expire.invoke(particles, 1249L);
        assertEquals(32, active.size());
        for (HitParticle particle : evicted) assertFalse(particle.isDead);
        expire.invoke(particles, 1250L);
        assertEquals(0, active.size());
        Field alpha = net.minecraft.client.particle.EntityFX.class.getDeclaredField("particleAlpha");
        alpha.setAccessible(true);
        for (HitParticle particle : evicted) {
            assertTrue(particle.isDead);
            assertEquals(0, alpha.getFloat(particle), 0);
        }
    }
}
