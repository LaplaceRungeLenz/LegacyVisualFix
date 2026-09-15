package com.legacyvisualfix.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TrailParticlesTest {

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
