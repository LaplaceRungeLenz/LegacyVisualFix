package com.modernnh.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UiMotionTest {

    @Test
    public void exponentialConvergenceIsIndependentOfFrameRate() {
        double slow = 0, fast = 0;
        for (int i = 0; i < 30; i++) slow = UiMotion.approach(slow, 100, 8, 1.0 / 30);
        for (int i = 0; i < 144; i++) fast = UiMotion.approach(fast, 100, 8, 1.0 / 144);
        assertEquals(100 * (1 - Math.exp(-8)), slow, 1e-9);
        assertEquals(slow, fast, 1e-9);
    }

    @Test
    public void reversalMovesImmediatelyAndNeverOvershoots() {
        double value = UiMotion.approach(80, 0, 14, .016);
        assertTrue(value > 0 && value < 80);
        assertEquals(80, UiMotion.approach(80, 0, 14, 0), 0);
        assertEquals(UiMotion.approach(80, 0, 14, .1), UiMotion.approach(80, 0, 14, 10), 0);
        assertEquals(80, UiMotion.approach(80, Double.NaN, 14, .1), 0);
        assertEquals(80, UiMotion.approach(80, 0, 14, Double.NaN), 0);
    }

    @Test
    public void springTracksReturnsAndResets() {
        UiMotion.Spring spring = new UiMotion.Spring();
        for (int i = 0; i < 60; i++) spring.update(18, 1.0 / 60);
        assertEquals(18, spring.value(), .2);
        for (int i = 0; i < 180; i++) spring.update(0, 1.0 / 60);
        assertEquals(0, spring.value(), .01);
        spring.update(25, .1);
        spring.reset();
        assertEquals(0, spring.value(), 0);
    }

    @Test
    public void springIsBoundedAndFrameRateComparable() {
        UiMotion.Spring slow = new UiMotion.Spring(), fast = new UiMotion.Spring();
        for (int i = 0; i < 15; i++) slow.update(25, 1.0 / 30);
        for (int i = 0; i < 72; i++) fast.update(25, 1.0 / 144);
        assertEquals(slow.value(), fast.value(), .3);
        for (int i = 0; i < 100; i++) {
            slow.update(i % 2 == 0 ? 10000 : -10000, 1000);
            assertTrue(Math.abs(slow.value()) <= 25);
        }
        double previous = slow.value();
        slow.update(0, Double.POSITIVE_INFINITY);
        slow.update(Double.NaN, .1);
        assertEquals(previous, slow.value(), 0);
    }
}
