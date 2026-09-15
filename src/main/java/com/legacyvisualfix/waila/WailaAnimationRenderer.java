package com.legacyvisualfix.waila;

import java.awt.Point;
import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.mixin.waila.AccessorTooltip;

import mcp.mobius.waila.overlay.OverlayConfig;
import mcp.mobius.waila.overlay.Tooltip;

/** Client render bridge. Waila's content measurements are never changed. */
public final class WailaAnimationRenderer {

    private static final TooltipAnimation ANIMATION = new TooltipAnimation();
    private static final IntBuffer SCISSOR = BufferUtils.createIntBuffer(16);
    private static Frame current;
    private static boolean drawn;
    private static Object world;
    private static int displayWidth, displayHeight, guiScale, anchorX, anchorY;
    private static float overlayScale;

    private WailaAnimationRenderer() {}

    public static void beginFrame() {
        drawn = false;
    }

    public static void endFrame() {
        if (!drawn) {
            ANIMATION.reset();
            world = null;
        }
    }

    public static Frame current() {
        return current;
    }

    public static Frame begin(Tooltip tooltip) {
        drawn = true;
        if (!WailaAnimationConfig.enabled || WailaAnimationConfig.durationMs <= 0) {
            ANIMATION.reset();
            world = null;
            return null;
        }
        // A recursive draw from another renderer should not replace the outer frame.
        if (current != null) return null;
        Minecraft mc = Minecraft.getMinecraft();
        AccessorTooltip data = (AccessorTooltip) tooltip;
        Point anchor = data.legacyvisualfix$getAnchor();
        int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        if (world != mc.theWorld || displayWidth != mc.displayWidth
            || displayHeight != mc.displayHeight
            || guiScale != scale
            || overlayScale != OverlayConfig.scale
            || anchorX != anchor.x
            || anchorY != anchor.y) {
            ANIMATION.reset();
        }
        world = mc.theWorld;
        displayWidth = mc.displayWidth;
        displayHeight = mc.displayHeight;
        guiScale = scale;
        overlayScale = OverlayConfig.scale;
        anchorX = anchor.x;
        anchorY = anchor.y;
        ANIMATION.update(
            data.legacyvisualfix$getWidth(),
            data.legacyvisualfix$getHeight(),
            System.nanoTime(),
            WailaAnimationConfig.durationMs);
        current = new Frame(
            data,
            (int) Math.round(ANIMATION.width()),
            (int) Math.round(ANIMATION.height()),
            scale * overlayScale);
        return current;
    }

    public static final class Frame implements AutoCloseable {

        public final int width, height;
        private final AccessorTooltip data;
        private final int originalX, originalY;
        private final double pixelScale;
        private boolean clipped;

        private Frame(AccessorTooltip data, int width, int height, double pixelScale) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.pixelScale = pixelScale;
            originalX = data.legacyvisualfix$getX();
            originalY = data.legacyvisualfix$getY();
            // Preserve Waila's percentage-of-free-space positioning, including exact final coordinates.
            data.legacyvisualfix$setX(
                originalX + (int) Math.round((data.legacyvisualfix$getWidth() - width) * anchorX / 10000.0));
            data.legacyvisualfix$setY(
                originalY + (int) Math.round((data.legacyvisualfix$getHeight() - height) * anchorY / 10000.0));
        }

        public void clipContent() {
            if (clipped) return;
            // Clip only while growing: shrinking already fits the new content, and settled frames
            // should preserve custom Waila renderers' original behavior exactly.
            if (width >= data.legacyvisualfix$getWidth() && height >= data.legacyvisualfix$getHeight()) return;
            int left = (int) Math.ceil((data.legacyvisualfix$getX() + 2) * pixelScale);
            int right = (int) Math.floor((data.legacyvisualfix$getX() + width - 1) * pixelScale);
            int bottom = displayHeight - (int) Math.floor((data.legacyvisualfix$getY() + height - 1) * pixelScale);
            int top = displayHeight - (int) Math.ceil((data.legacyvisualfix$getY() + 2) * pixelScale);
            if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
                SCISSOR.clear();
                GL11.glGetInteger(GL11.GL_SCISSOR_BOX, SCISSOR);
                left = Math.max(left, SCISSOR.get(0));
                bottom = Math.max(bottom, SCISSOR.get(1));
                right = Math.min(right, SCISSOR.get(0) + SCISSOR.get(2));
                top = Math.min(top, SCISSOR.get(1) + SCISSOR.get(3));
            }
            GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
            clipped = true;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
        }

        @Override
        public void close() {
            try {
                finishClip();
            } finally {
                data.legacyvisualfix$setX(originalX);
                data.legacyvisualfix$setY(originalY);
                current = null;
            }
        }

        public void finishClip() {
            if (clipped) {
                GL11.glPopAttrib();
                clipped = false;
            }
        }
    }
}
