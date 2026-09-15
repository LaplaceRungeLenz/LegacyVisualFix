package com.legacyvisualfix.smoke;

import java.nio.ByteBuffer;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.legacyvisualfix.render.ScreenCapture;

/** Verify actual GPU blend values, orientation and caller state, not just animation arithmetic. */
public final class CaptureSmoke {

    private CaptureSmoke() {}

    public static void run() {
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            OpenGlHelper.func_153171_g(GL30.GL_FRAMEBUFFER, 0);
            GL11.glDrawBuffer(GL11.GL_BACK);
            GL11.glReadBuffer(GL11.GL_BACK);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glClearColor(1, 0, 0, 1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(0, 0, Display.getWidth(), Display.getHeight() / 2);
            GL11.glClearColor(0, 1, 0, 1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            while (GL11.glGetError() != GL11.GL_NO_ERROR) {}
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            try (ScreenCapture frame = new ScreenCapture(GL11.GL_BACK)) {
                GL11.glClearColor(0, 0, 1, 1);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                frame.draw(0.5);
                assertPixel(Display.getHeight() * 3 / 4, 128, 0, 128);
                assertPixel(Display.getHeight() / 4, 0, 128, 128);
                if (GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) != texture
                    || GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != program
                    || GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING) != 0
                    || GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) != 0)
                    throw new AssertionError("Capture changed GL state");
            }
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) throw new AssertionError("Capture generated GL error " + error);
        } finally {
            OpenGlHelper.func_153171_g(GL30.GL_DRAW_FRAMEBUFFER, draw);
            OpenGlHelper.func_153171_g(GL30.GL_READ_FRAMEBUFFER, read);
            GL11.glPopAttrib();
        }
    }

    private static void assertPixel(int y, int r, int g, int b) {
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(Display.getWidth() / 2, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        if (Math.abs((pixel.get(0) & 255) - r) > 3 || Math.abs((pixel.get(1) & 255) - g) > 3
            || Math.abs((pixel.get(2) & 255) - b) > 3)
            throw new AssertionError(
                "Incorrect capture orientation or blend: " + (pixel.get(0) & 255)
                    + ","
                    + (pixel.get(1) & 255)
                    + ","
                    + (pixel.get(2) & 255));
    }
}
