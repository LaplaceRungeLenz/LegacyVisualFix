package com.legacyvisualfix.reload;

/** Completed listeners are work units, not estimates of elapsed time. Client-thread confined. */
public final class ReloadProgress {

    private boolean active;
    private int completed;
    private int total;
    private String stage = "";

    public void begin(int listeners) {
        active = true;
        completed = 0;
        total = Math.max(0, listeners);
        stage = "";
    }

    public void beforeListener(String name) {
        if (active) stage = name == null ? "" : name;
    }

    public void afterListener() {
        if (active && completed < total) completed++;
    }

    public void finish() {
        active = false;
        stage = "";
    }

    public boolean isActive() {
        return active;
    }

    public int getCompleted() {
        return completed;
    }

    public int getTotal() {
        return total;
    }

    public String getStage() {
        return stage;
    }

    public double getFraction() {
        return total == 0 ? 0 : (double) completed / total;
    }
}
