package com.legacyvisualfix.waila;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class PendingTooltipTest {

    @Test
    public void retainsCompleteContentUntilTheReplacementArrives() {
        PendingTooltip<Object> tooltip = new PendingTooltip<>();
        Object first = new Object(), second = new Object();
        assertSame(first, tooltip.resolve(first, false, 0));
        for (long time = 50_000_000; time <= 250_000_000; time += 50_000_000) {
            assertSame(first, tooltip.resolve(null, true, time));
        }
        assertSame(second, tooltip.resolve(second, false, 300_000_000));
        assertSame(second, tooltip.resolve(null, true, 350_000_000));
    }

    @Test
    public void waitingDoesNotRenewTheDeadline() {
        PendingTooltip<Object> tooltip = new PendingTooltip<>();
        Object first = new Object();
        tooltip.resolve(first, false, 0);
        assertSame(first, tooltip.resolve(null, true, 499_999_999));
        assertNull(tooltip.resolve(null, true, 500_000_000));
        assertNull(tooltip.resolve(null, true, 600_000_000));
    }

    @Test
    public void noTargetOrExplicitClearCannotResurrectOldContent() {
        PendingTooltip<Object> tooltip = new PendingTooltip<>();
        Object first = new Object();
        assertNull(tooltip.resolve(null, true, 0));
        tooltip.resolve(first, false, 0);
        assertNull(tooltip.resolve(null, false, 1));
        assertNull(tooltip.resolve(null, true, 2));
        tooltip.resolve(first, false, 3);
        tooltip.clear();
        assertNull(tooltip.resolve(null, true, 4));
    }
}
