package com.legacyvisualfix.waila;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.common.config.Configuration;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.wdmla.impl.ui.sizer.Area;
import com.gtnewhorizons.wdmla.impl.ui.sizer.Size;
import com.gtnewhorizons.wdmla.impl.ui.value.HUDRenderArea;

import mcp.mobius.waila.api.impl.ConfigHandler;
import mcp.mobius.waila.overlay.OverlayConfig;
import mcp.mobius.waila.utils.Constants;

/** Animates only the HUD render area, leaving WDMla component measurements untouched. */
public final class WdmlaAnimationRenderer {

    // WDMla temporarily clears mainHUD while a new target's server data is in flight.
    // Keep only the previous dimensions, not the old tooltip content, across short gaps.
    private static final int TARGET_GAP_GRACE_MS = 500;
    private static final TooltipAnimation ANIMATION = new TooltipAnimation();
    private static Frame current;
    private static boolean drawn;
    private static Object world, screen;
    private static int displayWidth, displayHeight, guiScale, anchorX, anchorY;
    private static float overlayScale;

    private WdmlaAnimationRenderer() {}

    public static void beginFrame() {
        drawn = false;
    }

    public static void endFrame() {
        if (drawn) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (world != mc.theWorld || screen != mc.currentScreen
            || !Minecraft.isGuiEnabled()
            || mc.gameSettings.keyBindPlayerList.getIsKeyPressed()
            || !ConfigHandler.instance()
                .showTooltip()
            || !WailaAnimationConfig.enabled
            || WailaAnimationConfig.durationMs <= 0) {
            reset();
        } else {
            expire(System.nanoTime());
        }
    }

    private static void expire(long now) {
        if (ANIMATION.resetIfIdle(now, TARGET_GAP_GRACE_MS)) {
            world = null;
            screen = null;
        }
    }

    private static void reset() {
        ANIMATION.reset();
        world = null;
        screen = null;
    }

    public static Frame begin(float width, float height) {
        drawn = true;
        if (current != null) return null;
        if (!WailaAnimationConfig.enabled || WailaAnimationConfig.durationMs <= 0) {
            reset();
            return null;
        }
        long now = System.nanoTime();
        // Also cover a pause in rendering where no empty-frame callback was delivered.
        expire(now);
        Minecraft mc = Minecraft.getMinecraft();
        int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        int x = ConfigHandler.instance()
            .getConfig(Configuration.CATEGORY_GENERAL, Constants.CFG_WAILA_POSX, 0);
        int y = ConfigHandler.instance()
            .getConfig(Configuration.CATEGORY_GENERAL, Constants.CFG_WAILA_POSY, 0);
        if (world != mc.theWorld || screen != mc.currentScreen
            || displayWidth != mc.displayWidth
            || displayHeight != mc.displayHeight
            || guiScale != scale
            || overlayScale != OverlayConfig.scale
            || anchorX != x
            || anchorY != y) {
            ANIMATION.reset();
        }
        world = mc.theWorld;
        screen = mc.currentScreen;
        displayWidth = mc.displayWidth;
        displayHeight = mc.displayHeight;
        guiScale = scale;
        overlayScale = OverlayConfig.scale;
        anchorX = x;
        anchorY = y;
        ANIMATION.update(width, height, now, WailaAnimationConfig.durationMs);
        current = new Frame(width, height);
        return current;
    }

    public static Size size(Size measured) {
        return current == null ? measured : current.size;
    }

    public static void beginContent() {
        if (current != null) current.beginContent();
    }

    public static void endContent() {
        if (current != null) current.endContent();
    }

    public static TooltipContentTransform contentTransform() {
        return current == null ? null : current.content;
    }

    public static double pixelScale() {
        return guiScale * overlayScale;
    }

    public static final class Frame implements AutoCloseable {

        private final Size size = new Size((float) ANIMATION.width(), (float) ANIMATION.height());
        private final float targetWidth, targetHeight;
        private TooltipContentTransform content;
        private boolean transformed;

        private Frame(float width, float height) {
            targetWidth = width;
            targetHeight = height;
        }

        private void beginContent() {
            if (content != null) return;
            Area fg = new HUDRenderArea(size).computeForeground();
            content = new TooltipContentTransform(
                fg.getX(),
                fg.getY(),
                size.getW(),
                size.getH(),
                targetWidth,
                targetHeight);
            if (content.scale == 1) return;
            GL11.glPushMatrix();
            transformed = true;
            GL11.glTranslatef(content.x, content.y, 0);
            GL11.glScalef(content.scale, content.scale, 1);
            GL11.glTranslatef(-content.x, -content.y, 0);
        }

        private void endContent() {
            if (transformed) {
                GL11.glPopMatrix();
                transformed = false;
            }
            content = null;
        }

        @Override
        public void close() {
            try {
                endContent();
            } finally {
                current = null;
            }
        }
    }
}
