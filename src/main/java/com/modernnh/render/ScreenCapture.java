package com.modernnh.render;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/** GPU-only snapshot. Read framebuffer and draw framebuffer are deliberately independent. */
public final class ScreenCapture implements AutoCloseable {

    private final int id;
    private final int width = Math.max(1, Display.getWidth());
    private final int height = Math.max(1, Display.getHeight());

    public ScreenCapture(int buffer) {
        id = GL11.glGenTextures();
        boolean fbo = OpenGlHelper.framebufferSupported;
        boolean separate = GLContext.getCapabilities().OpenGL30
            || GLContext.getCapabilities().GL_ARB_framebuffer_object;
        int target = separate ? GL30.GL_READ_FRAMEBUFFER : GL30.GL_FRAMEBUFFER;
        int binding = fbo ? GL11.glGetInteger(separate ? GL30.GL_READ_FRAMEBUFFER_BINDING : GL30.GL_FRAMEBUFFER_BINDING)
            : 0;
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int readBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int defaultReadBuffer = readBuffer;
        try {
            if (fbo) OpenGlHelper.func_153171_g(target, 0);
            defaultReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
            GL11.glReadBuffer(buffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, 0, 0, width, height, 0);
        } catch (RuntimeException | LinkageError e) {
            GL11.glDeleteTextures(id);
            throw e;
        } finally {
            GL11.glReadBuffer(defaultReadBuffer);
            if (fbo) OpenGlHelper.func_153171_g(target, binding);
            GL11.glReadBuffer(readBuffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }

    public void draw(double alpha) {
        int w = Math.max(1, Display.getWidth());
        int h = Math.max(1, Display.getHeight());
        try (GLState state = new GLState(w, h, w, h)) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glColor4d(1, 1, 1, Math.max(0, Math.min(1, alpha)));
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2d(0, 1);
            GL11.glVertex2d(0, 0);
            GL11.glTexCoord2d(0, 0);
            GL11.glVertex2d(0, h);
            GL11.glTexCoord2d(1, 0);
            GL11.glVertex2d(w, h);
            GL11.glTexCoord2d(1, 1);
            GL11.glVertex2d(w, 0);
            GL11.glEnd();
        }
    }

    @Override
    public void close() {
        GL11.glDeleteTextures(id);
    }
}
