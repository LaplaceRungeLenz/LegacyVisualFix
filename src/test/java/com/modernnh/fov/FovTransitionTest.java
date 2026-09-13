package com.modernnh.fov;

import static org.junit.Assert.*;

import org.junit.Test;

public class FovTransitionTest {

    @Test
    public void reachesNinetyFivePercentInConfiguredTime() {
        float value = 1;
        float alpha = FovTransition.coefficient(true, 300, 0.5F);
        for (int tick = 0; tick < 6; tick++) value += (1.15F - value) * alpha;
        assertEquals(1.1425F, value, 0.00001F);
    }

    @Test
    public void reversalRemainsContinuousAndDoesNotOvershoot() {
        float alpha = FovTransition.coefficient(true, 300, 0.5F);
        float value = 1 + 0.15F * alpha;
        assertTrue(value > 1 && value < 1.075F);
        for (int tick = 0; tick < 20; tick++) {
            float previous = value;
            value += (1 - value) * alpha;
            assertTrue(value >= 1 && value <= previous);
        }
    }

    @Test
    public void disabledAndInvalidSettingsPreserveIncomingCoefficient() {
        assertEquals(0.4F, FovTransition.coefficient(false, 300, 0.4F), 0);
        assertEquals(0.5F, FovTransition.coefficient(true, 0, 0.5F), 0);
        assertEquals(0.5F, FovTransition.coefficient(true, -1, 0.5F), 0);
    }
}
