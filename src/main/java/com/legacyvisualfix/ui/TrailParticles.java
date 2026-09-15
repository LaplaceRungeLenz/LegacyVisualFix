package com.legacyvisualfix.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** GUI-space trail simulation, independent of rendering and game classes. */
public final class TrailParticles {

    public static final class Particle {

        public double x, y, age, lifetime, size, rotation;
        public int rgb;
    }

    private final List<Particle> active = new ArrayList<Particle>();
    private final List<Particle> view = Collections.unmodifiableList(active);
    private final Random random = new Random();
    private double emissionRemainder;

    /** Real elapsed time expires particles even after a pause. */
    public void update(double dt) {
        if (!Double.isFinite(dt) || dt <= 0) return;
        for (Iterator<Particle> it = active.iterator(); it.hasNext();) {
            Particle particle = it.next();
            particle.age += dt;
            if (particle.age >= particle.lifetime) it.remove();
        }
    }

    /**
     * Emit at up to rate particles/second and one particle per three GUI pixels.
     * Fractional emission carries across moving frames; pauses never build a backlog.
     */
    public void emit(double fromX, double fromY, double toX, double toY, int rgb, double dt, int maxParticles,
        double rate) {
        int limit = Math.max(0, Math.min(512, maxParticles));
        while (active.size() > limit) active.remove(0);
        if (!Double.isFinite(fromX) || !Double.isFinite(fromY)
            || !Double.isFinite(toX)
            || !Double.isFinite(toY)
            || !Double.isFinite(dt)
            || !Double.isFinite(rate)
            || dt <= 0
            || dt > .1
            || rate <= 0
            || limit == 0) {
            emissionRemainder = 0;
            return;
        }
        double distance = Math.hypot(toX - fromX, toY - fromY);
        if (!Double.isFinite(distance) || distance < .0001) {
            emissionRemainder = 0;
            return;
        }
        double amount = Math.min(distance / 3, Math.min(rate, 5120) * dt);
        double previous = emissionRemainder;
        int count = (int) Math.floor(previous + amount + 1e-10);
        emissionRemainder = Math.max(0, previous + amount - count);
        // Drop excess demand instead of accumulating a future burst.
        for (int i = 0; i < count; i++) {
            double fraction = Math.min(1, (i + 1 - previous) / amount);
            Particle particle = new Particle();
            particle.x = fromX * (1 - fraction) + toX * fraction;
            particle.y = fromY * (1 - fraction) + toY * fraction;
            particle.lifetime = .4 + random.nextDouble() * .3;
            particle.size = 1.5 + random.nextDouble() * 1.5;
            particle.rotation = random.nextDouble() * 360;
            particle.rgb = rgb & 0xFFFFFF;
            if (active.size() == limit) active.remove(0);
            active.add(particle);
        }
    }

    public List<Particle> particles() {
        return view;
    }

    public void clear() {
        active.clear();
        emissionRemainder = 0;
    }
}
