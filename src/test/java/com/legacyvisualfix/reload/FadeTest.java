package com.legacyvisualfix.reload;

import static org.junit.Assert.*;

import org.junit.Test;

public class FadeTest {

    @Test
    public void endpointsMidpointAndDisabledTransitions() {
        assertEquals(0, Fade.in(0, 200), 0);
        assertEquals(0.5, Fade.in(100, 200), 0);
        assertEquals(1, Fade.in(300, 200), 0);
        assertEquals(0, Fade.in(-10, 200), 0);
        assertEquals(1, Fade.in(0, 0), 0);
        assertEquals(1, Fade.out(0, 300), 0);
        assertEquals(0.5, Fade.out(150, 300), 0);
        assertEquals(0, Fade.out(400, 300), 0);
        assertEquals(0, Fade.out(0, 0), 0);
    }
}
