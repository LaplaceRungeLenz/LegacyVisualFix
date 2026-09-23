package com.legacyvisualfix.vajra;

/** Reject malformed or out-of-reach network requests before touching world state. */
public final class VajraClickValidation {

    private VajraClickValidation() {}

    public static boolean valid(int face, float x, float y, float z, double distanceSquared, double reach) {
        return face >= 0 && face < 6
            && unit(x)
            && unit(y)
            && unit(z)
            && distanceSquared >= 0
            && distanceSquared <= reach * reach;
    }

    private static boolean unit(float value) {
        return value >= 0 && value <= 1;
    }
}
