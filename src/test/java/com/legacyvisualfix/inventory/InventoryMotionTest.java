package com.legacyvisualfix.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InventoryMotionTest {

    @Test
    public void slowFirstRenderDoesNotConsumeTheVisibleEntrance() {
        InventoryMotion motion = new InventoryMotion();
        motion.initialize(true, 250, 32);
        assertEquals(32, motion.frame(0, 400, 100), 0);
        motion.rendered(600_000_000L);
        assertEquals(4, motion.frame(725_000_000L, 400, 100), 0);
        assertTrue(motion.isEntering());
        assertEquals(0, motion.frame(850_000_000L, 400, 100), 0);
        assertFalse(motion.isEntering());
    }

    @Test
    public void laterRenderCompletionsDoNotRestartTheClock() {
        InventoryMotion motion = new InventoryMotion();
        motion.initialize(true, 250, 32);
        motion.rendered(500_000_000L); // No animated frame has been drawn yet.
        motion.frame(600_000_000L, 400, 100);
        motion.rendered(610_000_000L);
        assertEquals(4, motion.frame(735_000_000L, 400, 100), 0);
        motion.rendered(745_000_000L);
        assertEquals(0, motion.frame(860_000_000L, 400, 100), 0);
        motion.rendered(870_000_000L);
        assertFalse(motion.isEntering());
    }

    @Test
    public void firstRenderCompletionDoesNotUndoAnInputCancellation() {
        InventoryMotion motion = new InventoryMotion();
        motion.initialize(true, 250, 32);
        motion.frame(0, 400, 100);
        assertTrue(motion.mouse(0, true, 0));
        motion.rendered(600_000_000L);
        assertEquals(0, motion.frame(616_000_000L, 400, 100), 0);
        assertFalse(motion.isEntering());
        assertTrue(motion.blocksHeldMouse());
        assertTrue(motion.mouse(0, false, 0));
        assertFalse(motion.blocksHeldMouse());
    }

    @Test
    public void freshObjectsReturningFromOtherScreensDoNotReplay() {
        InventoryMotion returned = new InventoryMotion();
        returned.openingFrom(false, null);
        returned.initialize(true, 250, 32);
        assertFalse(returned.isEntering());
    }

    @Test
    public void worldOpeningCarriesThroughInitialCreativeRedirectOnly() {
        InventoryMotion initial = new InventoryMotion();
        initial.openingFrom(true, null);
        InventoryMotion creative = new InventoryMotion();
        creative.openingFrom(false, initial);
        creative.initialize(true, 250, 32);
        assertTrue(creative.isEntering());
        InventoryMotion returned = new InventoryMotion();
        returned.openingFrom(false, creative);
        returned.initialize(true, 250, 32);
        assertFalse(returned.isEntering());
    }

    @Test
    public void returningThroughCreativeRedirectDoesNotStartAnimation() {
        InventoryMotion returned = new InventoryMotion();
        returned.openingFrom(false, null);
        InventoryMotion creative = new InventoryMotion();
        creative.openingFrom(false, returned);
        creative.initialize(true, 250, 32);
        assertFalse(creative.isEntering());
    }

    @Test
    public void startsAtFirstFrameAndSettlesWithoutOvershoot() {
        InventoryMotion motion = new InventoryMotion();
        motion.initialize(true, 250, 0);
        assertTrue(motion.isEntering());
        assertEquals(364, motion.frame(10_000_000_000L, 400, 100), 0.001);
        assertEquals(45.5, motion.frame(10_125_000_000L, 400, 100), 0.001);
        assertEquals(0, motion.frame(10_250_000_000L, 400, 100), 0);
        assertFalse(motion.isEntering());
    }

    @Test
    public void cadenceDoesNotChangePosition() {
        InventoryMotion slow = new InventoryMotion();
        InventoryMotion fast = new InventoryMotion();
        slow.initialize(true, 250, 32);
        fast.initialize(true, 250, 32);
        slow.frame(0, 400, 100);
        fast.frame(0, 400, 100);
        for (long t = 1; t < 125_000_000; t += 1_000_000) fast.frame(t, 400, 100);
        assertEquals(slow.frame(125_000_000, 400, 100), fast.frame(125_000_000, 400, 100), 0);
    }

    @Test
    public void resizeDoesNotReplayAndDisabledIsImmediate() {
        InventoryMotion motion = new InventoryMotion();
        motion.initialize(true, 250, 0);
        motion.frame(0, 400, 100);
        motion.initialize(true, 250, 0);
        assertEquals(0, motion.frame(1, 500, 150), 0);
        InventoryMotion disabled = new InventoryMotion();
        disabled.initialize(false, 250, 0);
        assertFalse(disabled.isEntering());
        InventoryMotion zero = new InventoryMotion();
        zero.initialize(true, 0, 0);
        assertEquals(0, zero.frame(0, 400, 100), 0);
    }

    @Test
    public void consumedPressAlsoConsumesReleaseAndDragging() {
        InventoryMotion motion = new InventoryMotion();
        motion.initialize(true, 250, 0);
        assertFalse(motion.mouse(-1, false, 0)); // movement must not finish animation
        assertTrue(motion.isEntering());
        assertTrue(motion.mouse(0, true, 0));
        assertFalse(motion.isEntering());
        assertTrue(motion.blocksHeldMouse());
        assertTrue(motion.mouse(-1, false, 0));
        assertTrue(motion.mouse(0, false, 0));
        assertFalse(motion.blocksHeldMouse());
        assertFalse(motion.mouse(0, true, 0));
    }

    @Test
    public void wheelFinishesAndOnlyConsumesOneAction() {
        InventoryMotion motion = new InventoryMotion();
        motion.initialize(true, 250, 0);
        assertTrue(motion.mouse(-1, false, 120));
        assertEquals(0, motion.frame(0, 400, 100), 0);
        assertFalse(motion.mouse(-1, false, 120));
    }

    @Test
    public void imeTextWithoutKeyDownFinishesButModifiersAndCloseBypass() {
        InventoryMotion motion = new InventoryMotion();
        motion.initialize(true, 250, 0);
        assertFalse(motion.key(true, 42, '\0', true));
        assertTrue(motion.isEntering());
        assertFalse(motion.key(true, 1, '\0', true));
        assertTrue(motion.key(false, 0, '中', false));
        assertFalse(motion.isEntering());
        assertFalse(motion.key(false, 0, '文', false));
    }
}
