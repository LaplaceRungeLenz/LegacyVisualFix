package com.legacyvisualfix.waila;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TooltipAnimationTest {

    @Test
    public void shortServerDataGapsKeepThePreviousSizeInBothDirections() {
        TooltipAnimation animation = new TooltipAnimation();
        animation.update(100, 40, 0, 150);
        for (long time = 16_000_000; time < 250_000_000; time += 16_000_000) {
            assertFalse(animation.resetIfIdle(time, 500));
        }
        animation.update(200, 80, 250_000_000, 150);
        assertEquals(100, animation.width(), 0.001);
        animation.update(200, 80, 325_000_000, 150);
        assertEquals(150, animation.width(), 0.001);
        animation.update(200, 80, 400_000_000, 150);
        assertFalse(animation.resetIfIdle(600_000_000, 500));
        animation.update(100, 40, 600_000_000, 150);
        assertEquals(200, animation.width(), 0.001);
        animation.update(100, 40, 675_000_000, 150);
        assertEquals(150, animation.width(), 0.001);
    }

    @Test
    public void emptyFramesDoNotExtendTheIdleDeadline() {
        TooltipAnimation animation = new TooltipAnimation();
        animation.update(100, 40, 0, 150);
        assertFalse(animation.resetIfIdle(250_000_000, 500));
        assertFalse(animation.resetIfIdle(499_999_999, 500));
        assertTrue(animation.resetIfIdle(500_000_000, 500));
        animation.update(200, 80, 510_000_000, 150);
        assertEquals(200, animation.width(), 0.001);
    }

    @Test
    public void activeFramesRefreshIdleDeadlineAndExplicitResetStillSnaps() {
        TooltipAnimation animation = new TooltipAnimation();
        animation.update(100, 40, 0, 150);
        animation.update(100, 40, 400_000_000, 150);
        assertFalse(animation.resetIfIdle(700_000_000, 500));
        animation.reset();
        animation.update(200, 80, 710_000_000, 150);
        assertEquals(200, animation.width(), 0.001);
    }

    @Test
    public void preservesFractionalWdmlaComponentSizes() {
        TooltipAnimation animation = new TooltipAnimation();
        animation.update(100.25, 40.5, 0, 100);
        animation.update(200.75, 80.25, 0, 100);
        animation.update(200.75, 80.25, 100_000_000, 100);
        assertEquals(200.75, animation.width(), 0.0001);
        assertEquals(80.25, animation.height(), 0.0001);
    }

    @Test
    public void interpolatesThenFinishesWithoutRestartingForEqualTargets() {
        TooltipAnimation animation = new TooltipAnimation();
        animation.update(100, 40, 0, 100);
        animation.update(200, 80, 0, 100);
        animation.update(200, 80, 50_000_000, 100);
        assertEquals(150, animation.width(), 0.001);
        assertEquals(60, animation.height(), 0.001);
        animation.update(200, 80, 100_000_000, 100);
        assertEquals(200, animation.width(), 0.001);
    }

    @Test
    public void retargetsFromCurrentPositionAndCanShrink() {
        TooltipAnimation animation = new TooltipAnimation();
        animation.update(100, 40, 0, 100);
        animation.update(200, 80, 0, 100);
        animation.update(50, 20, 50_000_000, 100);
        assertEquals(150, animation.width(), 0.001);
        animation.update(50, 20, 100_000_000, 100);
        assertEquals(100, animation.width(), 0.001);
        animation.update(50, 20, 150_000_000, 100);
        assertEquals(50, animation.width(), 0.001);
    }

    @Test
    public void elapsedTimeNotFrameCountDeterminesSize() {
        TooltipAnimation slow = new TooltipAnimation();
        TooltipAnimation fast = new TooltipAnimation();
        for (TooltipAnimation animation : new TooltipAnimation[] { slow, fast }) {
            animation.update(100, 40, 0, 150);
            animation.update(200, 80, 0, 150);
        }
        for (long time = 0; time < 75_000_000; time += 33_333_333) slow.update(200, 80, time, 150);
        for (long time = 0; time < 75_000_000; time += 6_944_444) fast.update(200, 80, time, 150);
        slow.update(200, 80, 75_000_000, 150);
        fast.update(200, 80, 75_000_000, 150);
        assertEquals(150, slow.width(), 0.001);
        assertEquals(slow.width(), fast.width(), 0.001);
    }

    @Test
    public void resetAndDisabledDurationSnapToNewTarget() {
        TooltipAnimation animation = new TooltipAnimation();
        animation.update(100, 40, 0, 100);
        assertEquals(100, animation.width(), 0.001);
        animation.reset();
        animation.update(200, 80, 1, 100);
        assertEquals(200, animation.width(), 0.001);
        animation.update(50, 20, 2, 0);
        assertEquals(50, animation.width(), 0.001);
        assertEquals(20, animation.height(), 0.001);
    }
}
