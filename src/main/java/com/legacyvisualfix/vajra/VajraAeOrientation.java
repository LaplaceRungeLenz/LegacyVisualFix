/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Generic AE orientation adapted from PinkYuDeer/GTNH-Qol-Improvements.
 * See META-INF/NOTICE-Vajra.md.
 */
package com.legacyvisualfix.vajra;

import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.util.IOrientable;

/** Maps a selected grid face to the orientation convention of the AE target. */
public final class VajraAeOrientation {

    private VajraAeOrientation() {}

    public static boolean rotate(IOrientable orientable, ForgeDirection direction, boolean sneaking,
        boolean interfaceOutput) {
        if (!orientable.canBeRotated()) {
            return false;
        }

        if (interfaceOutput) {
            // TileInterface uses the opposite of Up as its output (pointAt).
            // Use AE2's canonical perpendicular Forward and its native setter so
            // connection masks, neighbor notifications and client updates remain intact.
            // An interface has no independent roll: sneaking selects the same output face.
            orientable.setOrientation(
                direction.offsetY != 0 ? ForgeDirection.SOUTH : ForgeDirection.UP,
                direction.getOpposite());
            return true;
        }

        ForgeDirection front = orientable.getForward();
        ForgeDirection up = orientable.getUp();
        if (front == ForgeDirection.UNKNOWN) {
            front = direction.offsetY == 0 ? ForgeDirection.UP : ForgeDirection.NORTH;
        }
        if (up == ForgeDirection.UNKNOWN || up == front || up == front.getOpposite()) {
            up = front.offsetY == 0 ? ForgeDirection.UP : ForgeDirection.NORTH;
        }
        if (sneaking) {
            up = up.getRotation(front);
        } else {
            front = direction;
            if (up == front || up == front.getOpposite()) {
                up = front.offsetY == 0 ? ForgeDirection.UP : ForgeDirection.NORTH;
            }
        }
        orientable.setOrientation(front, up);
        return true;
    }

}
