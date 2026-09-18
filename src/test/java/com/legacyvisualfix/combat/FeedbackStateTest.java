package com.legacyvisualfix.combat;

import static org.junit.Assert.*;

import org.junit.Test;

public class FeedbackStateTest {

    @Test
    public void rejectsDuplicatesAndExpiresWithoutDelayingANewHit() {
        FeedbackState state = new FeedbackState();
        assertTrue(state.accept(new HitFeedbackMessage(2, 0, 3, 2, 0), 1000));
        assertFalse(state.accept(new HitFeedbackMessage(2, 0, 3, 2, 0), 1050));
        assertFalse(state.accept(new HitFeedbackMessage(1, 0, 3, 2, 0), 1050));
        assertEquals(0.5, state.alpha(1080, 160), 0.0001);
        assertEquals(0, state.alpha(1160, 160), 0);
        assertTrue(state.accept(new HitFeedbackMessage(3, 0, 3, 0, 4), 1100));
        assertEquals(1, state.alpha(1100, 160), 0);
        assertEquals(4, state.absorbed, 0);
        state.reset();
        assertEquals(0, state.alpha(1100, 160), 0);
        assertTrue(state.accept(new HitFeedbackMessage(1, 0, 3, 1, 0), 1100));
    }
}
