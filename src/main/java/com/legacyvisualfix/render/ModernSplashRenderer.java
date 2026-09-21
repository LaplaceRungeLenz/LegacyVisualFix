package com.legacyvisualfix.render;

import org.lwjgl.opengl.Display;

import com.legacyvisualfix.LegacyVisualFix;
import com.legacyvisualfix.reload.ModernSplashAccess;
import com.legacyvisualfix.reload.ReloadProgress;

import cpw.mods.fml.common.Loader;

/** Owns the optional session and guards all upstream GL calls, including texture allocation/deletion. */
public final class ModernSplashRenderer implements AutoCloseable {

    private ModernSplashAccess api;
    private boolean active;

    public void initialize() {
        boolean loaded = Loader.isModLoaded("modernsplash");
        api = ModernSplashAccess.find(loaded);
        if (loaded) LegacyVisualFix.LOG.info(
            api == null ? "ModernSplash has no compatible runtime API; keeping LegacyVisualFix reload screen"
                : "ModernSplash runtime API detected; using its reload screen");
    }

    public void begin() {
        if (api == null) return;
        try (GLState state = state()) {
            active = api.begin();
        } catch (RuntimeException | LinkageError e) {
            LegacyVisualFix.LOG.warn("Cannot initialize ModernSplash reload renderer; using default", e);
            active = false;
        }
    }

    public boolean draw(ReloadProgress progress, String detail, String title, boolean present) {
        if (!active) return false;
        try (GLState state = state()) {
            api.render(title, progress.getStage() + "  " + detail, progress.getCompleted(), progress.getTotal());
            if (present) Display.update(false);
        } catch (RuntimeException | LinkageError e) {
            LegacyVisualFix.LOG.warn("ModernSplash reload rendering failed; using default for this session", e);
            close();
            return false;
        }
        Display.processMessages();
        return true;
    }

    private static GLState state() {
        int width = Math.max(1, Display.getWidth());
        int height = Math.max(1, Display.getHeight());
        return new GLState(width, height, width, height);
    }

    @Override
    public void close() {
        if (!active) return;
        active = false;
        try (GLState state = state()) {
            api.end();
        } catch (RuntimeException | LinkageError e) {
            LegacyVisualFix.LOG.warn("Cannot release ModernSplash runtime textures", e);
        }
    }
}
