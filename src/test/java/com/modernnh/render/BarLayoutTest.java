package com.modernnh.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BarLayoutTest {

    @Test
    public void centeredBarShrinksAndStaysOnTinyScreens() {
        BarLayout layout = new BarLayout(80, 40, 240, 12, 1, 1);
        assertTrue(layout.x >= 0 && layout.y >= 0);
        assertTrue(layout.x + layout.width <= 80);
        assertTrue(layout.y + layout.height <= 40);
        assertEquals(layout.width / 2, layout.filledWidth(0.5), 0.0001);
        assertEquals(0, layout.filledWidth(-1), 0);
        assertEquals(layout.width, layout.filledWidth(2), 0);
    }
}
