package com.modernnh.waila.chromatic;

import java.awt.Dimension;
import java.awt.Point;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import com.modernnh.mixin.waila.AccessorTooltip;
import com.modernnh.waila.TooltipAnimation;
import com.modernnh.waila.WailaAnimationConfig;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.util.SectionBox;

import mcp.mobius.waila.overlay.OverlayConfig;
import mcp.mobius.waila.overlay.Tooltip;

/** Scoped to Compat's Waila call; inventory tooltips never participate. */
public final class ChromaticAnimation {

    private static final TooltipAnimation ANIMATION = new TooltipAnimation();
    private static Tooltip tooltip;
    private static Frame frame;
    private static boolean drawn;
    private static Object world, renderer;
    private static int screenWidth, screenHeight, scale, anchorX, anchorY;
    private static float wailaScale;

    private ChromaticAnimation() {}

    public static void beginOverlay(Tooltip value) {
        tooltip = value;
        drawn = false;
    }

    public static void endOverlay() {
        tooltip = null;
        frame = null;
        if (!drawn) {
            ANIMATION.reset();
            world = null;
            renderer = null;
        }
    }

    public static Frame begin(TooltipContext context, int x, int y) {
        if (tooltip == null || frame != null || !"waila".equals(context.getContextName())) return null;
        drawn = true;
        if (!WailaAnimationConfig.enabled || WailaAnimationConfig.durationMs <= 0) {
            ANIMATION.reset();
            world = null;
            renderer = null;
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Point anchor = ((AccessorTooltip) tooltip).modernnh$getAnchor();
        Dimension size = context.getTooltipSize();
        if (size == null) return null;
        if (world != mc.theWorld || renderer != context.getRenderer()
            || screenWidth != mc.displayWidth
            || screenHeight != mc.displayHeight
            || scale != context.getScaleFactor()
            || wailaScale != OverlayConfig.scale
            || anchorX != anchor.x
            || anchorY != anchor.y) ANIMATION.reset();
        world = mc.theWorld;
        renderer = context.getRenderer();
        screenWidth = mc.displayWidth;
        screenHeight = mc.displayHeight;
        scale = context.getScaleFactor();
        wailaScale = OverlayConfig.scale;
        anchorX = anchor.x;
        anchorY = anchor.y;
        ANIMATION.update(size.width, size.height, System.nanoTime(), WailaAnimationConfig.durationMs);
        int width = (int) Math.round(ANIMATION.width());
        int height = (int) Math.round(ANIMATION.height());
        double scaleShift = (double) scale
            / new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        frame = new Frame(
            context.getActivePageComponent(),
            x + (int) Math.round((size.width - width) * anchor.x / 10000.0),
            y + (int) Math.round((size.height - height) * anchor.y / 10000.0),
            (int) Math.round((width - size.width) / scaleShift),
            (int) Math.round((height - size.height) / scaleShift));
        return frame;
    }

    public static Frame forBox(Object box) {
        return frame != null && frame.root == box ? frame : null;
    }

    public static final class Frame implements AutoCloseable {

        public final SectionBox root;
        public final int x, y, deltaWidth, deltaHeight;
        public int clipX, clipY, clipWidth, clipHeight;
        public boolean hasClip;

        private Frame(SectionBox root, int x, int y, int deltaWidth, int deltaHeight) {
            this.root = root;
            this.x = x;
            this.y = y;
            this.deltaWidth = deltaWidth;
            this.deltaHeight = deltaHeight;
        }

        @Override
        public void close() {
            frame = null;
        }
    }
}
