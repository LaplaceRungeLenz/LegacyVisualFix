package com.legacyvisualfix.waila;

import java.util.Set;

/** WDMla advertises both its own mod ID and the legacy Waila API. */
public enum WailaBackend {

    NONE,
    WAILA,
    WDMLA;

    public static WailaBackend select(Set<String> loadedMods) {
        if (loadedMods.contains("wdmla")) return WDMLA;
        return loadedMods.contains("Waila") ? WAILA : NONE;
    }
}
