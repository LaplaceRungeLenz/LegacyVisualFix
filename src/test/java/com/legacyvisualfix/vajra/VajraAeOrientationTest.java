package com.legacyvisualfix.vajra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import appeng.api.util.IOrientable;

public class VajraAeOrientationTest {

    @Test
    public void interfaceOutputMatchesAllSixSelectedFacesRegardlessOfPreviousOrientationOrSneaking() {
        // TileInterface.setOrientation defines pointAt = Up.getOpposite(), not Forward.
        ForgeDirection[] expectedUp = { ForgeDirection.UP, ForgeDirection.DOWN, ForgeDirection.SOUTH,
            ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.WEST };
        for (ForgeDirection previous : ForgeDirection.values()) {
            for (boolean sneaking : new boolean[] { false, true }) {
                for (ForgeDirection selected : ForgeDirection.VALID_DIRECTIONS) {
                    RecordingOrientation tile = new RecordingOrientation(
                        previous == ForgeDirection.UNKNOWN ? ForgeDirection.UNKNOWN
                            : previous.offsetY != 0 ? ForgeDirection.SOUTH : ForgeDirection.UP,
                        previous.getOpposite());
                    assertTrue(VajraAeOrientation.rotate(tile, selected, sneaking, true));
                    assertEquals("Up for output " + selected, expectedUp[selected.ordinal()], tile.up);
                    assertEquals(selected, tile.up.getOpposite());
                    assertTrue(tile.front != tile.up && tile.front != tile.up.getOpposite());
                    ForgeDirection front = tile.front;
                    // Repeating the selected grid cell must not cycle to another output.
                    assertTrue(VajraAeOrientation.rotate(tile, selected, sneaking, true));
                    assertEquals(expectedUp[selected.ordinal()], tile.up);
                    assertEquals(front, tile.front);
                }
            }
        }
    }

    @Test
    public void nonInterfaceStillUsesForwardForFacingAndSneakingForRoll() {
        RecordingOrientation tile = new RecordingOrientation(ForgeDirection.NORTH, ForgeDirection.UP);
        assertTrue(VajraAeOrientation.rotate(tile, ForgeDirection.EAST, false, false));
        assertEquals(ForgeDirection.EAST, tile.front);
        assertEquals(ForgeDirection.UP, tile.up);
        assertTrue(VajraAeOrientation.rotate(tile, ForgeDirection.SOUTH, true, false));
        assertEquals(ForgeDirection.EAST, tile.front);
        assertEquals(ForgeDirection.NORTH, tile.up);
    }

    @Test
    public void unrotatableTargetIsUnchanged() {
        RecordingOrientation tile = new RecordingOrientation(ForgeDirection.NORTH, ForgeDirection.UP);
        tile.rotatable = false;
        assertFalse(VajraAeOrientation.rotate(tile, ForgeDirection.WEST, false, true));
        assertEquals(0, tile.writes);
    }

    private static final class RecordingOrientation implements IOrientable {

        private ForgeDirection front;
        private ForgeDirection up;
        private boolean rotatable = true;
        private int writes;

        private RecordingOrientation(ForgeDirection front, ForgeDirection up) {
            this.front = front;
            this.up = up;
        }

        @Override
        public boolean canBeRotated() {
            return rotatable;
        }

        @Override
        public ForgeDirection getForward() {
            return front;
        }

        @Override
        public ForgeDirection getUp() {
            return up;
        }

        @Override
        public void setOrientation(ForgeDirection forward, ForgeDirection upward) {
            front = forward;
            up = upward;
            writes++;
        }
    }
}
