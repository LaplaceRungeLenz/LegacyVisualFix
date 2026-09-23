package com.legacyvisualfix.combat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CombatInboxTest {

    @Before
    @After
    public void clear() {
        CombatInbox.clear();
    }

    @Test
    public void rejectsWrongConnectionDimensionAndExpiredDelivery() {
        Object connection = new Object();
        CombatInbox.offer(new HitFeedbackMessage(1, -1, 2, 1, 0), connection);
        CombatInbox.Entry entry = CombatInbox.poll();
        assertTrue(entry.matches(connection, -1, entry.receivedNs));
        assertFalse(entry.matches(new Object(), -1, entry.receivedNs));
        assertFalse(entry.matches(connection, 0, entry.receivedNs));
        assertFalse(entry.matches(connection, -1, entry.receivedNs + 500_000_001L));
    }

    @Test
    public void boundsPendingWorkAndClearsPendingHitsOnWorldReset() {
        Object connection = new Object();
        for (int i = 1; i <= 400; i++) CombatInbox.offer(new HitFeedbackMessage(i, 0, 2, 1, 0), connection);
        int count = 0;
        while (CombatInbox.poll() != null) count++;
        assertEquals(256, count);
        CombatInbox.offer(new HitFeedbackMessage(401, 0, 2, 1, 0), connection);
        CombatInbox.clear();
        assertNull(CombatInbox.poll());
    }
}
