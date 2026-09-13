package com.modernnh.waila;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TooltipAnimationTest {

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
