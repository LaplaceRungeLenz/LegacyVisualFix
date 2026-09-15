package com.legacyvisualfix.inventory;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;

import org.lwjgl.opengl.GL11;

/** No layout mutation: screen-space overlays remain outside these short render scopes. */
public final class InventoryAnimations {

    private InventoryAnimations() {}

    public static InventoryMotion motion(Object screen) {
        if (screen == null
            || (screen.getClass() != GuiInventory.class && screen.getClass() != GuiContainerCreative.class))
            return null;
        return ((InventoryMotionAccess) screen).legacyvisualfix$inventoryMotion();
    }

    public static boolean entering(Object screen) {
        InventoryMotion motion = motion(screen);
        return motion != null && motion.isEntering();
    }

    public static void draw(GuiScreen screen, Runnable draw) {
        InventoryMotion motion = motion(screen);
        float offset = motion == null ? 0 : motion.offset();
        if (offset == 0) {
            draw.run();
            return;
        }
        // Only touch the model-view stack; no framebuffer, viewport or projection changes.
        int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glTranslatef(0, offset, 0);
        try {
            draw.run();
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(mode);
        }
    }
}
