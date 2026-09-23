package com.legacyvisualfix.combat;

import static org.junit.Assert.*;

import org.junit.Test;

public class DamageTransactionsTest {

    @Test
    public void observesHealthAndAbsorptionWithoutTreatingHealingAsDamage() {
        DamageTransactions ledger = new DamageTransactions();
        Object target = new Object();
        DamageTransactions.Scope scope = ledger.begin(target, 20, 4);
        DamageTransactions.Result result = ledger.finish(scope, 17, 1, true);
        assertEquals(3, result.health, 0);
        assertEquals(3, result.absorbed, 0);
        assertTrue(result.hasDamage());
        assertFalse(
            ledger.finish(ledger.begin(target, 17, 1), 20, 4, true)
                .hasDamage());
        assertFalse(
            ledger.finish(ledger.begin(target, 20, 0), 20, 0, true)
                .hasDamage());
    }

    @Test
    public void excludesNestedSameTargetDamageEvenAcrossDifferentTargetScopes() {
        DamageTransactions ledger = new DamageTransactions();
        Object a = new Object(), b = new Object();
        DamageTransactions.Scope outer = ledger.begin(a, 20, 4);
        DamageTransactions.Scope middle = ledger.begin(b, 10, 0);
        DamageTransactions.Scope inner = ledger.begin(a, 18, 3);
        assertEquals(3, ledger.finish(inner, 15, 1, true).health, 0);
        assertEquals(2, ledger.finish(middle, 8, 0, true).health, 0);
        DamageTransactions.Result result = ledger.finish(outer, 14, 0, true);
        assertEquals(3, result.health, 0);
        assertEquals(2, result.absorbed, 0);
        assertFalse(ledger.active());
    }

    @Test
    public void exceptionsCleanUpAndDoNotReattributePartialNestedDamage() {
        DamageTransactions ledger = new DamageTransactions();
        Object target = new Object();
        DamageTransactions.Scope outer = ledger.begin(target, 20, 0);
        DamageTransactions.Scope inner = ledger.begin(target, 20, 0);
        assertFalse(
            ledger.finish(inner, 17, 0, false)
                .hasDamage());
        assertFalse(
            ledger.finish(outer, 17, 0, true)
                .hasDamage());
        assertFalse(ledger.active());
    }

    @Test
    public void rejectsNonfiniteSamplesAndClampsNegativeHealth() {
        DamageTransactions ledger = new DamageTransactions();
        Object target = new Object();
        assertFalse(
            ledger.finish(ledger.begin(target, Float.NaN, 0), 1, 0, true)
                .hasDamage());
        assertFalse(
            ledger.finish(ledger.begin(target, 20, 0), Float.NEGATIVE_INFINITY, 0, true)
                .hasDamage());
        assertEquals(2, ledger.finish(ledger.begin(target, 2, 0), -10, 0, true).health, 0);
    }
}
