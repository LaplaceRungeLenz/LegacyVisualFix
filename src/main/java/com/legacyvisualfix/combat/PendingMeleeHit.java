package com.legacyvisualfix.combat;

/** Correlates a local attempt with a subsequent client-visible hurt signal, not damage ownership. */
public final class PendingMeleeHit {

    private long attemptedAt;
    private float health;
    private int hurtTime;
    private boolean consumed;

    public PendingMeleeHit(float health, int hurtTime, long nowMs) {
        this.health = health;
        this.hurtTime = hurtTime;
        attemptedAt = nowMs;
    }

    /** A new attempt extends the window without discarding an unobserved hurt signal. */
    public void retry(long nowMs) {
        if (!expired(nowMs)) attemptedAt = nowMs;
    }

    public boolean expired(long nowMs) {
        return consumed || nowMs < attemptedAt || nowMs - attemptedAt > 500;
    }

    public boolean observe(float currentHealth, int currentHurtTime, long nowMs) {
        if (expired(nowMs) || !Float.isFinite(currentHealth)) return false;
        boolean hurt = currentHealth < health || currentHurtTime > hurtTime;
        health = currentHealth;
        hurtTime = currentHurtTime;
        consumed = hurt;
        return hurt;
    }
}
