package com.modernnh.vajra;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VajraClickValidationTest {

    @Test
    public void acceptsAllFacesAndEdgeHitsWithinReach() {
        for (int face = 0; face < 6; face++) {
            assertTrue(VajraClickValidation.valid(face, 0, 0.5F, 1, 25, 5));
        }
    }

    @Test
    public void rejectsInvalidFacesCoordinatesAndReach() {
        assertFalse(VajraClickValidation.valid(-1, 0, 0, 0, 0, 5));
        assertFalse(VajraClickValidation.valid(6, 0, 0, 0, 0, 5));
        for (float invalid : new float[] { Float.NaN, Float.POSITIVE_INFINITY, -0.01F, 1.01F }) {
            assertFalse(VajraClickValidation.valid(1, invalid, 0, 0, 0, 5));
            assertFalse(VajraClickValidation.valid(1, 0, invalid, 0, 0, 5));
            assertFalse(VajraClickValidation.valid(1, 0, 0, invalid, 0, 5));
        }
        assertFalse(VajraClickValidation.valid(1, 0, 0, 0, 25.01, 5));
        assertFalse(VajraClickValidation.valid(1, 0, 0, 0, Double.NaN, 5));
    }
}
