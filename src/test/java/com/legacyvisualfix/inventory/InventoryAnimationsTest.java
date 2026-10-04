package com.legacyvisualfix.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import makamys.satchels.gui.GuiSatchelsInventory;

public class InventoryAnimationsTest {

    @Test
    public void recognizesExactSatchelsReplacementWithoutHardDependency() {
        GuiSatchelsInventory screen = new GuiSatchelsInventory();
        assertSame(screen.legacyvisualfix$inventoryMotion(), InventoryAnimations.motion(screen));
    }

    @Test
    public void excludesUnknownScreensAndSubclasses() {
        assertNull(InventoryAnimations.motion(null));
        assertNull(InventoryAnimations.motion(new Object()));
        assertNull(InventoryAnimations.motion(new GuiSatchelsInventory() {}));
        assertNull(InventoryAnimations.motion((InventoryMotionAccess) InventoryMotion::new));
    }

    @Test
    public void replacementOpenedFromWorldAnimatesThenSettles() {
        GuiSatchelsInventory screen = new GuiSatchelsInventory();
        InventoryMotion motion = InventoryAnimations.motion(screen);
        assertNotNull("Satchels replacement must reach the shared animation lifecycle", motion);
        motion.openingFrom(true, null);
        motion.initialize(true, 250, 32);
        assertTrue(InventoryAnimations.entering(screen));
        assertEquals(32, motion.frame(0, 400, 100), 0);
        assertEquals(4, motion.frame(125_000_000, 400, 100), 0);
        assertEquals(0, motion.frame(250_000_000, 400, 100), 0);
        assertFalse(InventoryAnimations.entering(screen));
    }

    @Test
    public void replacementReturningFromEquipmentDoesNotReplay() {
        InventoryMotion motion = InventoryAnimations.motion(new GuiSatchelsInventory());
        assertNotNull(motion);
        // Equipment is outside the allow-list, so InventoryScreenEvents supplies no previous motion.
        motion.openingFrom(false, null);
        motion.initialize(true, 250, 32);
        assertFalse(motion.isEntering());
        assertEquals(0, motion.frame(0, 400, 100), 0);
    }

    @Test
    public void replacementConsumesFirstClickAndReleaseBeforeAllowingNextAction() {
        for (int button : new int[] { 0, 1 }) {
            InventoryMotion motion = InventoryAnimations.motion(new GuiSatchelsInventory());
            assertNotNull(motion);
            motion.openingFrom(true, null);
            motion.initialize(true, 250, 32);
            assertTrue(motion.mouse(button, true, 0));
            assertFalse(motion.isEntering());
            assertTrue(motion.blocksHeldMouse());
            assertTrue(motion.mouse(button, false, 0));
            assertFalse(motion.blocksHeldMouse());
            assertFalse(motion.mouse(button, true, 0));
        }
    }
}
