package com.legacyvisualfix.waila;

import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.common.config.Configuration;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.wdmla.impl.ui.sizer.Area;
import com.gtnewhorizons.wdmla.impl.ui.sizer.Size;
import com.gtnewhorizons.wdmla.impl.ui.value.HUDRenderArea;

import mcp.mobius.waila.api.impl.ConfigHandler;
import mcp.mobius.waila.overlay.OverlayConfig;
import mcp.mobius.waila.utils.Constants;

/** Animates only the HUD render area, leaving WDMla component measurements untouched. */
public final class WdmlaAnimationRenderer {

    private static final TooltipAnimation ANIMATION = new TooltipAnimation();
    private static final IntBuffer SCISSOR = BufferUtils.createIntBuffer(16);
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
        if (!drawn) reset();
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
        ANIMATION.update(width, height, System.nanoTime(), WailaAnimationConfig.durationMs);
        current = new Frame(width, height);
        return current;
    }

    public static Size size(Size measured) {
        return current == null ? measured : current.size;
    }

    public static void clipContent() {
        if (current != null) current.clip();
    }

    public static void finishClip() {
        if (current != null) current.unclip();
    }

    public static final class Frame implements AutoCloseable {

        private final Size size = new Size((float) ANIMATION.width(), (float) ANIMATION.height());
        private final float targetWidth, targetHeight;
        private boolean clipped;

        private Frame(float width, float height) {
            targetWidth = width;
            targetHeight = height;
        }

        private void clip() {
            if (clipped || (size.getW() >= targetWidth && size.getH() >= targetHeight)) return;
            Area bg = new HUDRenderArea(size).computeBackground();
            double pixels = guiScale * overlayScale;
            int left = (int) Math.ceil((bg.getX() + 1) * pixels);
            int right = (int) Math.floor((bg.getX() + bg.getW() - 1) * pixels);
            int bottom = displayHeight - (int) Math.floor((bg.getY() + bg.getH() - 1) * pixels);
            int top = displayHeight - (int) Math.ceil((bg.getY() + 1) * pixels);
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

        private void unclip() {
            if (clipped) {
                GL11.glPopAttrib();
                clipped = false;
            }
        }

        @Override
        public void close() {
            try {
                unclip();
            } finally {
                current = null;
            }
        }
    }
}
