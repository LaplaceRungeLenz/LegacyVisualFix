package com.legacyvisualfix.waila;

/** Retains complete content only while a replacement is waiting for server data. */
public final class PendingTooltip<T> {

    private static final long MAX_WAIT_NANOS = 500_000_000L;
    private T complete;
    private long lastComplete;

    public T resolve(T next, boolean waitingForData, long now) {
        if (next != null) {
            complete = next;
            lastComplete = now;
            return next;
        }
        if (!waitingForData || now - lastComplete >= MAX_WAIT_NANOS) clear();
        return complete;
    }

    public void clear() {
        complete = null;
    }
}
