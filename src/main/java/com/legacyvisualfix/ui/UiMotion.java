package com.legacyvisualfix.ui;

public final class UiMotion {

    private UiMotion() {}

    /** Exact exponential convergence; a paused frame advances by at most 100 ms. */
    public static double approach(double current, double target, double speed, double dt) {
        if (!Double.isFinite(current)) return Double.isFinite(target) ? target : 0;
        if (!Double.isFinite(target) || !Double.isFinite(speed) || !Double.isFinite(dt) || speed <= 0 || dt <= 0)
            return current;
        double weight = -Math.expm1(-speed * Math.min(dt, .1));
        return current * (1 - weight) + target * weight;
    }

    /** Damped rotation in degrees, integrated with bounded steps for low frame rates. */
    public static final class Spring {

        private double angle;
        private double velocity;

        public double update(double targetDegrees, double dt) {
            if (!Double.isFinite(targetDegrees) || !Double.isFinite(dt) || dt <= 0) return angle;
            double target = Math.max(-25, Math.min(25, targetDegrees));
            double elapsed = Math.min(dt, .1);
            int steps = (int) Math.ceil(elapsed * 240);
            double step = elapsed / steps;
            for (int i = 0; i < steps; i++) {
                velocity += ((target - angle) * 220 - velocity * 21) * step;
                angle += velocity * step;
                if (angle > 25) {
                    angle = 25;
                    velocity = Math.min(0, velocity);
                }
                if (angle < -25) {
                    angle = -25;
                    velocity = Math.max(0, velocity);
                }
            }
            return angle;
        }

        public double value() {
            return angle;
        }

        public void reset() {
            angle = 0;
            velocity = 0;
        }
    }
}
