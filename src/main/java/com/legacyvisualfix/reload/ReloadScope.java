package com.legacyvisualfix.reload;

/** Suppresses nested resource manager sessions without disturbing their execution. */
public final class ReloadScope {

    private int depth;

    public boolean enter() {
        return ++depth == 1;
    }

    public boolean exit() {
        if (depth == 0) return false;
        return --depth == 0;
    }

    public boolean isOutermost() {
        return depth == 1;
    }
}
