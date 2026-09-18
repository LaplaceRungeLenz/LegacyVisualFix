package com.legacyvisualfix.combat;

import static org.junit.Assert.*;

import org.junit.Test;

public class ParticleBudgetTest {

    @Test
    public void reclaimsEvictedParticlesWithoutDependingOnTheirDeadFlag() {
        assertFalse(ParticleBudget.activeExpired(1000, 1000));
        assertFalse(ParticleBudget.activeExpired(1000, 1249));
        assertTrue(ParticleBudget.activeExpired(1000, 1250));
        assertTrue(ParticleBudget.activeExpired(1000, 5000));
        assertTrue(ParticleBudget.activeExpired(1000, 999));
        assertTrue(ParticleBudget.activeExpired(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    @Test
    public void clampsHugeRequestsWithoutScalingByDamage() {
        ParticleBudget budget = new ParticleBudget();
        assertEquals(8, budget.claim(Integer.MAX_VALUE, 0, Float.MAX_VALUE, Float.MAX_VALUE, 1000));
        assertEquals(0, budget.claim(-1, 0, 1, 0, 1000));
        assertEquals(0, budget.claim(0, 0, 1, 0, 1000));
    }

    @Test
    public void invalidDamageNeverConsumesBudget() {
        ParticleBudget budget = new ParticleBudget();
        assertEquals(0, budget.claim(8, 0, Float.NaN, 1, 1000));
        assertEquals(0, budget.claim(8, 0, 1, Float.POSITIVE_INFINITY, 1000));
        assertEquals(0, budget.claim(8, 0, -1, 1, 1000));
        assertEquals(0, budget.claim(8, 0, 1, -1, 1000));
        assertEquals(0, budget.claim(8, 0, 0, 0, 1000));
        for (int i = 0; i < 4; i++) assertEquals(8, budget.claim(8, 0, 0, 2, 1000));
    }

    @Test
    public void burstExpiresPerParticleWithoutFixedWindowBoundarySpikes() {
        ParticleBudget budget = new ParticleBudget();
        assertEquals(8, budget.claim(8, 0, 1, 0, 1000));
        for (int i = 0; i < 3; i++) assertEquals(8, budget.claim(8, 0, 1, 0, 1100));
        assertEquals(0, budget.claim(8, 0, 1, 0, 1199));
        assertEquals(8, budget.claim(8, 0, 1, 0, 1200));
        assertEquals(0, budget.claim(8, 0, 1, 0, 1200));
        for (int i = 0; i < 3; i++) assertEquals(8, budget.claim(8, 0, 1, 0, 1300));
        assertEquals(0, budget.claim(8, 0, 1, 0, 1300));
    }

    @Test
    public void settingsSuppressOrReduceAndResetClearsPriorWorld() {
        ParticleBudget budget = new ParticleBudget();
        assertEquals(0, budget.claim(6, 2, 1, 0, 1000));
        assertEquals(0, budget.claim(6, -1, 1, 0, 1000));
        assertEquals(3, budget.claim(6, 1, 1, 0, 1000));
        assertEquals(1, budget.claim(1, 1, 1, 0, 1000));
        for (int i = 0; i < 3; i++) assertEquals(8, budget.claim(8, 0, 1, 0, 1000));
        assertEquals(4, budget.claim(8, 0, 1, 0, 1000));
        budget.reset();
        for (int i = 0; i < 4; i++) assertEquals(8, budget.claim(8, 0, 1, 0, 1000));
        assertEquals(0, budget.claim(8, 0, 1, 0, 1000));
        assertEquals(8, budget.claim(8, 0, 1, 0, 10));
    }

    @Test
    public void autoIsFailClosedWhenEfrIndicatorStateIsUnknown() {
        assertTrue(ParticleBudget.allowed("auto", false, null));
        assertTrue(ParticleBudget.allowed("auto", true, Boolean.FALSE));
        assertFalse(ParticleBudget.allowed("auto", true, Boolean.TRUE));
        assertFalse(ParticleBudget.allowed("auto", true, null));
        assertTrue(ParticleBudget.allowed("always", true, null));
        assertFalse(ParticleBudget.allowed("off", false, Boolean.FALSE));
        assertFalse(ParticleBudget.allowed("unknown", false, Boolean.FALSE));
        assertFalse(ParticleBudget.allowed(null, false, null));
    }
}
