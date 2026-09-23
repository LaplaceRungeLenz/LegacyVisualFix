package com.legacyvisualfix.combat;

/** Client-thread state with monotonic time supplied by the caller. */
public final class FeedbackState {

    private long sequence;
    private long receivedMs;
    public float health, absorbed;

    public boolean accept(HitFeedbackMessage message, long nowMs) {
        if (!message.valid() || message.sequence <= sequence) return false;
        sequence = message.sequence;
        receivedMs = nowMs;
        health = message.health;
        absorbed = message.absorbed;
        return true;
    }

    public float alpha(long nowMs, int durationMs) {
        if (sequence == 0 || durationMs <= 0) return 0;
        return Math.max(0, 1 - (float) Math.max(0, nowMs - receivedMs) / durationMs);
    }

    public void reset() {
        sequence = 0;
        receivedMs = 0;
        health = absorbed = 0;
    }
}
