package com.legacyvisualfix.combat;

/** Local presentation signal. Weights are effect inputs, never authoritative damage amounts. */
public final class HitFeedback {

    public final long sequence;
    public final int dimension, targetId;
    public final float health, absorbed;

    public HitFeedback(long sequence, int dimension, int targetId, float health, float absorbed) {
        this.sequence = sequence;
        this.dimension = dimension;
        this.targetId = targetId;
        this.health = health;
        this.absorbed = absorbed;
    }

    public boolean valid() {
        return sequence > 0 && Float.isFinite(health)
            && Float.isFinite(absorbed)
            && health >= 0
            && absorbed >= 0
            && (health > 0 || absorbed > 0);
    }
}
