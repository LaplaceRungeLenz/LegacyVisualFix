package com.legacyvisualfix.waila;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

public class WailaBackendTest {

    @Test
    public void replacementWinsOverItsLegacyApiModId() {
        assertEquals(WailaBackend.WDMLA, WailaBackend.select(new HashSet<>(Arrays.asList("Waila", "wdmla"))));
        assertEquals(WailaBackend.WDMLA, WailaBackend.select(Collections.singleton("wdmla")));
    }

    @Test
    public void originalAndAbsentModsRemainSupported() {
        assertEquals(WailaBackend.WAILA, WailaBackend.select(Collections.singleton("Waila")));
        assertEquals(WailaBackend.NONE, WailaBackend.select(Collections.emptySet()));
    }
}
