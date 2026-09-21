package com.legacyvisualfix.smoke;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.reload.ReloadScreen;

import cpw.mods.fml.common.Loader;

/** Exercises the real optional API and GPU; never included in the distribution. */
final class ModernSplashSmoke {

    private ModernSplashSmoke() {}

    static void assertActive() throws Exception {
        if (!Loader.isModLoaded("modernsplash")) return;
        Object adapter = field(ReloadScreen.class, "MODERN_SPLASH").get(null);
        if (!field(adapter.getClass(), "active").getBoolean(adapter)) {
            throw new AssertionError("ModernSplash did not own the reload frame");
        }
    }

    static void run(File results) throws Exception {
        if (!Loader.isModLoaded("modernsplash")) return;
        Class<?> api = Class.forName("gkappa.modernsplash.RuntimeSplash");
        Class<?> splash = Class.forName("gkappa.modernsplash.CustomSplash");
        Object oldFont = splash.getField("fontTexture")
            .get(null);
        Object oldLogo = splash.getField("logoTexture")
            .get(null);
        Object oldThread = splash.getField("thread")
            .get(null);
        long startupTime = Class.forName("gkappa.modernsplash.ModernSplash")
            .getField("doneTime")
            .getLong(null);
        ReloadScreen.runShaderReload(() -> {
            try {
                assertActive();
                int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
                // Fail after the real renderer has pushed matrices (memory bar font call).
                Object renderer = field(api, "renderer").get(null);
                field(renderer.getClass(), "fontRenderer").set(renderer, null);
                ReloadScreen.shaderStep("Intentional renderer failure");
                Object adapter = field(ReloadScreen.class, "MODERN_SPLASH").get(null);
                if (field(adapter.getClass(), "active").getBoolean(adapter))
                    throw new AssertionError("Failed backend remained active");
                if (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) != depth)
                    throw new AssertionError("Failed renderer leaked a matrix stack entry");
                if (field(ReloadScreen.class, "failed").getBoolean(null))
                    throw new AssertionError("Default backend did not recover");
                ClientSmoke.screenshot(new File(results, "modernsplash-fallback.png"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ReloadScreen.runShaderReload(() -> {
            try {
                assertActive();
                ReloadScreen.runShaderPipeline(() -> ReloadScreen.shaderStep("Nested shader compilation"));
                assertActive();
                ClientSmoke.screenshot(new File(results, "modernsplash-shader.png"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (field(api, "owner").get(null) != null || field(api, "renderer").get(null) != null)
            throw new AssertionError("Runtime resources leaked after completion");
        if (oldFont != splash.getField("fontTexture")
            .get(null) || oldLogo
                != splash.getField("logoTexture")
                    .get(null)
            || oldThread != splash.getField("thread")
                .get(null)
            || startupTime != Class.forName("gkappa.modernsplash.ModernSplash")
                .getField("doneTime")
                .getLong(null))
            throw new AssertionError("Runtime sessions modified startup state");
        Files.write(
            new File(results, "modernsplash-result.txt").toPath(),
            "PASS: real runtime backend, nested shaders, render failure fallback, GL stack recovery, session cleanup and startup state isolation\n"
                .getBytes(StandardCharsets.UTF_8));
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
