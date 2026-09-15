package com.legacyvisualfix.render;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/** Preserve the caller's framebuffer, shader, texture units, matrices and fixed-function state. */
final class GLState implements AutoCloseable {

    private final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final boolean shaders = GLContext.getCapabilities().OpenGL20;
    private final int program = shaders ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
    private final boolean fbo = OpenGlHelper.framebufferSupported;
    private final boolean separateFbo = GLContext.getCapabilities().OpenGL30
        || GLContext.getCapabilities().GL_ARB_framebuffer_object;
    private final int framebuffer = fbo ? GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING) : 0;
    private final int framebufferTarget = separateFbo ? GL30.GL_DRAW_FRAMEBUFFER : GL30.GL_FRAMEBUFFER;

    GLState(int width, int height, int logicalWidth, int logicalHeight) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        if (fbo) OpenGlHelper.func_153171_g(framebufferTarget, 0);
        if (shaders) GL20.glUseProgram(0);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, logicalWidth, logicalHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glViewport(0, 0, width, height);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColorMask(true, true, true, true);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        GL11.glDrawBuffer(GL11.GL_BACK);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
    }

    @Override
    public void close() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        if (fbo) OpenGlHelper.func_153171_g(framebufferTarget, framebuffer);
        if (shaders) GL20.glUseProgram(program);
        GL11.glPopAttrib();
        OpenGlHelper.setActiveTexture(activeTexture);
        GL11.glMatrixMode(matrixMode);
    }
}
