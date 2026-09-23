package com.legacyvisualfix.combat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PendingMeleeHitTest {

    @Test
    public void requiresNewHurtSignalAndConsumesOnlyOnce() {
        PendingMeleeHit hit = new PendingMeleeHit(20, 8, 1000);
        assertFalse(hit.observe(20, 8, 1010));
        assertFalse(hit.observe(20, 7, 1020));
        assertTrue(hit.observe(20, 10, 1050));
        assertFalse(hit.observe(18, 10, 1060));
    }

    @Test
    public void repeatedAttackKeepsUnobservedHurtSignalAndRenewsWindow() {
        PendingMeleeHit hit = new PendingMeleeHit(20, 0, 1000);
        // Hurt arrived before tick END; another attack must not replace the original baseline.
        hit.retry(1450);
        assertTrue(hit.observe(18, 10, 1510));
        hit.retry(1520);
        assertFalse(hit.observe(16, 10, 1530));
    }

    @Test
    public void healthLossCanConfirmWithoutHurtAnimation() {
        PendingMeleeHit hit = new PendingMeleeHit(20, 0, 1000);
        assertFalse(hit.observe(22, 0, 1020));
        assertTrue(hit.observe(21, 0, 1030));
    }

    @Test
    public void staleAndBackwardsTimeNeverConfirm() {
        assertFalse(new PendingMeleeHit(20, 0, 1000).observe(18, 10, 1501));
        assertFalse(new PendingMeleeHit(20, 0, 1000).observe(18, 10, 999));
        assertTrue(new PendingMeleeHit(20, 0, 1000).expired(1501));
    }

    @Test
    public void invalidHealthDoesNotCreateFeedback() {
        PendingMeleeHit hit = new PendingMeleeHit(20, 0, 1000);
        assertFalse(hit.observe(Float.NaN, 10, 1010));
        assertTrue(hit.observe(18, 10, 1020));
    }
}
