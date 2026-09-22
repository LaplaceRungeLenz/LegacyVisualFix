package com.legacyvisualfix.waila;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TooltipContentTransformTest {

    @Test
    public void fitsBothDimensionsWithoutStretchingOrEnlarging() {
        assertEquals(0.25, new TooltipContentTransform(0, 0, 50, 80, 200, 40).scale, 0.0001);
        assertEquals(0.5, new TooltipContentTransform(0, 0, 200, 20, 200, 40).scale, 0.0001);
        assertEquals(1, new TooltipContentTransform(0, 0, 200, 80, 100, 40).scale, 0.0001);
    }

    @Test
    public void modelViewportUsesSamePivotAndInvertedScreenYAsText() {
        TooltipContentTransform content = new TooltipContentTransform(100, 200, 50, 40, 100, 80);
        // Two physical pixels per GUI unit; origin is (200, 600) from the bottom of a 1000px display.
        assertEquals(220, content.viewportX(240, 2));
        assertEquals(560, content.viewportY(520, 2, 1000));
        assertEquals(20, content.viewportSize(40));
        assertEquals(200, content.viewportX(200, 2));
        assertEquals(600, content.viewportY(600, 2, 1000));
    }

    @Test
    public void emptyContentIsFiniteAndSettledViewportIsUnchanged() {
        TooltipContentTransform content = new TooltipContentTransform(12.5f, 25.5f, 0, 0, 0, 0);
        assertEquals(1, content.scale, 0);
        assertEquals(81, content.viewportX(81, 1.5));
        assertEquals(301, content.viewportY(301, 1.5, 1000));
        assertEquals(31, content.viewportSize(31));
    }
}
