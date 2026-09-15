package com.legacyvisualfix.compat;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;

/** This query is safe even before Loader's mod map exists. */
public final class StartupCompatibility {

    private StartupCompatibility() {}

    public static boolean modDiscoveryComplete() {
        return Loader.instance()
            .hasReachedState(LoaderState.CONSTRUCTING);
    }
}
