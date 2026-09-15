package com.legacyvisualfix.ui;

import net.minecraft.client.gui.inventory.GuiContainer;

import codechicken.nei.guihook.GuiContainerManager;

/** Loaded only after NEI is present; respect overlays that obscure real slots. */
final class NeiUiBridge {

    private NeiUiBridge() {}

    static boolean obscured(Object gui, int x, int y) {
        if (!(gui instanceof GuiContainer)) return false;
        GuiContainerManager manager = GuiContainerManager.getManager((GuiContainer) gui);
        return manager != null && manager.objectUnderMouse(x, y);
    }
}
