package com.legacyvisualfix.theme;

import java.util.Properties;

/** Preserve every custom key; only an exact stock configuration is eligible. */
public final class ThemeMigration {

    private ThemeMigration() {}

    public static boolean upgrade(Properties current, Properties previous, Properties next) {
        if (previous.isEmpty() || !current.equals(previous)) return false;
        current.clear();
        current.putAll(next);
        return true;
    }
}
