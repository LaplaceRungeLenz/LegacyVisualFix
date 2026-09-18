package com.legacyvisualfix.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TrailParticlesTest {

    @Test
    public void starsTwinkleWithinBoundsAndShrinkToNothing() {
        TrailParticles.Particle p = new TrailParticles.Particle();
        p.lifetime = .7;
        p.size = 4;
        assertEquals(0, p.opacity(), 0);
        double previous = 0;
        boolean brightenedAfterFadeIn = false;
        for (int i = 1; i <= 70; i++) {
            p.age = i * .01;
            double opacity = p.opacity();
            assertTrue(opacity >= 0 && opacity <= 1);
            if (i > 15 && opacity > previous + .005) brightenedAfterFadeIn = true;
            previous = opacity;
        }
        assertTrue("brightness should twinkle, not only fade", brightenedAfterFadeIn);
        assertEquals(0, p.opacity(), 1e-9);
        assertEquals(0, p.radius(), 1e-9);
    }

    @Test
    public void starsDriftBackwardsAndRotateAfterEmission() {
        TrailParticles trail = new TrailParticles();
        trail.emit(0, 0, 60, 0, 0xffffff, .05, 20, 60);
        TrailParticles.Particle p = trail.particles()
            .get(0);
        double x = p.x, rotation = p.rotation;
        trail.update(.1);
        assertTrue("stars should drift behind the cursor", p.x < x);
        assertTrue("stars should rotate", Math.abs(p.rotation - rotation) > .001);
    }

    private TrailParticles line(int frames) {
        TrailParticles trail = new TrailParticles();
        for (int i = 0; i < frames; i++)
            trail.emit(i * 300.0 / frames, 0, (i + 1) * 300.0 / frames, 0, 0x123456, 1.0 / frames, 512, 60);
        return trail;
    }

    @Test
    public void continuousPathEmitsComparableCountsAtDifferentFrameRates() {
        TrailParticles slow = line(30), fast = line(144);
        assertTrue(
            slow.particles()
                .size() >= 50);
        assertEquals(
            slow.particles()
                .size(),
            fast.particles()
                .size(),
            1);
        for (TrailParticles.Particle p : slow.particles()) {
            assertTrue(p.x >= 0 && p.x <= 300);
            assertEquals(0, p.y, 0);
            assertEquals(0x123456, p.rgb);
            assertTrue(p.lifetime >= .4 && p.lifetime <= .7);
        }
    }

    @Test
    public void invalidAndStationaryFramesEmitNothingAndStallsDoNotAccumulate() {
        TrailParticles trail = new TrailParticles();
        trail.emit(0, 0, 0, 0, 0, .05, 100, 60);
        trail.emit(0, 0, 100, 0, 0, 0, 100, 60);
        trail.emit(0, 0, Double.NaN, 0, 0, .05, 100, 60);
        trail.emit(0, 0, 100, 0, 0, 10, 100, 60);
        assertTrue(
            trail.particles()
                .isEmpty());
        trail.emit(0, 0, 10, 0, 0, 1.0 / 60, 100, 60);
        assertTrue(
            trail.particles()
                .size() <= 2);
    }

    @Test
    public void poolIsBoundedExpiresAndClears() {
        TrailParticles trail = new TrailParticles();
        for (int i = 0; i < 100; i++) trail.emit(0, 0, 100, 100, 0, .1, 12, 10000);
        assertEquals(
            12,
            trail.particles()
                .size());
        trail.update(Double.NaN);
        assertEquals(
            12,
            trail.particles()
                .size());
        trail.update(2);
        assertTrue(
            trail.particles()
                .isEmpty());
        trail.emit(0, 0, 100, 0, 0, .1, 12, 60);
        assertFalse(
            trail.particles()
                .isEmpty());
        trail.clear();
        assertTrue(
            trail.particles()
                .isEmpty());
    }

    @Test
    public void subframeEmissionIsInterpolatedAcrossPath() {
        TrailParticles trail = new TrailParticles();
        trail.emit(0, 10, 120, 10, 0, .1, 100, 60);
        assertTrue(
            trail.particles()
                .size() >= 2);
        double previous = -1;
        for (TrailParticles.Particle p : trail.particles()) {
            assertTrue(p.x > previous);
            previous = p.x;
        }
        assertTrue(
            trail.particles()
                .get(0).x < 100);
    }
}
