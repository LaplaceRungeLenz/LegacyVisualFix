package com.legacyvisualfix.combat;

/** Pure presentation policy: at most 32 motes in any rolling 200 ms window. */
public final class ParticleBudget {

    private final long[] times = new long[32];
    private int head, size;
    private long lastTime;

    public static boolean activeExpired(long emittedAt, long nowMs) {
        long elapsed = nowMs - emittedAt;
        // Reclaim on clock rollback too; negative elapsed with increasing time means subtraction overflow.
        return nowMs < emittedAt || elapsed < 0 || elapsed >= 250;
    }

    public int claim(int requested, int setting, float health, float absorbed, long nowMs) {
        if (requested <= 0 || setting < 0
            || setting >= 2
            || !Float.isFinite(health)
            || !Float.isFinite(absorbed)
            || health < 0
            || absorbed < 0
            || (health == 0 && absorbed == 0)) return 0;
        if (nowMs < lastTime) reset();
        lastTime = nowMs;
        while (size > 0 && nowMs - times[head] >= 200) {
            head = (head + 1) % times.length;
            size--;
        }
        int count = Math.min(8, requested);
        if (setting == 1) count = Math.max(1, count / 2);
        count = Math.min(count, times.length - size);
        for (int i = 0; i < count; i++) {
            times[(head + size) % times.length] = nowMs;
            size++;
        }
        return count;
    }

    public void reset() {
        head = size = 0;
        lastTime = 0;
    }

    public static boolean allowed(String mode, boolean efrInstalled, Boolean efrDamageEnabled) {
        return "always".equals(mode)
            || ("auto".equals(mode) && (!efrInstalled || Boolean.FALSE.equals(efrDamageEnabled)));
    }
}
