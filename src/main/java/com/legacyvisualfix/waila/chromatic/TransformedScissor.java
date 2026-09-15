package com.legacyvisualfix.waila.chromatic;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/** Clips in framebuffer space after the resource pack's translation/scale transform. */
public final class TransformedScissor implements AutoCloseable {

    private static final FloatBuffer MODEL = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer POINT = BufferUtils.createFloatBuffer(4);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static final IntBuffer PREVIOUS = BufferUtils.createIntBuffer(16);

    public TransformedScissor(int x, int y, int width, int height) {
        MODEL.clear();
        PROJECTION.clear();
        VIEWPORT.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODEL);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
        float left = Float.POSITIVE_INFINITY, bottom = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY, top = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 4; corner++) {
            POINT.clear();
            GLU.gluProject(
                x + ((corner & 1) == 0 ? 0 : Math.max(0, width)),
                y + ((corner & 2) == 0 ? 0 : Math.max(0, height)),
                0,
                MODEL,
                PROJECTION,
                VIEWPORT,
                POINT);
            left = Math.min(left, POINT.get(0));
            right = Math.max(right, POINT.get(0));
            bottom = Math.min(bottom, POINT.get(1));
            top = Math.max(top, POINT.get(1));
        }
        if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
            PREVIOUS.clear();
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, PREVIOUS);
            left = Math.max(left, PREVIOUS.get(0));
            right = Math.min(right, PREVIOUS.get(0) + PREVIOUS.get(2));
            bottom = Math.max(bottom, PREVIOUS.get(1));
            top = Math.min(top, PREVIOUS.get(1) + PREVIOUS.get(3));
        }
        int l = (int) Math.ceil(left), b = (int) Math.ceil(bottom);
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(l, b, Math.max(0, (int) Math.floor(right) - l), Math.max(0, (int) Math.floor(top) - b));
    }

    @Override
    public void close() {
        GL11.glPopAttrib();
    }
}
