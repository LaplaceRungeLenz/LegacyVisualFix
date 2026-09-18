package com.legacyvisualfix.combat;

import java.util.concurrent.ArrayBlockingQueue;

/** No client class references: the message handler can be registered on a dedicated server. */
public final class CombatInbox {

    private static final ArrayBlockingQueue<Entry> QUEUE = new ArrayBlockingQueue<>(256);

    private CombatInbox() {}

    public static void offer(HitFeedbackMessage message, Object connection) {
        if (message.valid()) QUEUE.offer(new Entry(message, connection, System.nanoTime()));
    }

    public static Entry poll() {
        return QUEUE.poll();
    }

    public static void clear() {
        QUEUE.clear();
    }

    public static final class Entry {

        public final HitFeedbackMessage message;
        public final Object connection;
        public final long receivedNs;

        private Entry(HitFeedbackMessage message, Object connection, long receivedNs) {
            this.message = message;
            this.connection = connection;
            this.receivedNs = receivedNs;
        }

        public boolean matches(Object currentConnection, int dimension, long nowNs) {
            return connection == currentConnection && message.dimension == dimension
                && nowNs - receivedNs <= 500_000_000L;
        }
    }
}
