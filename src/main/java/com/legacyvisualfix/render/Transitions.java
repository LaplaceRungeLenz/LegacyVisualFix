package com.legacyvisualfix.render;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.reload.Fade;

/** Entry blends with the last presented frame; exit blends over freshly rendered game frames. */
public final class Transitions implements AutoCloseable {

    private ScreenCapture exit;
    private long exitStart;
    private int exitDuration;

    public void enter(int duration, Runnable renderWithoutSwap) {
        close();
        if (duration == 0) {
            renderWithoutSwap.run();
            Display.update(false);
            return;
        }
        try (ScreenCapture previous = new ScreenCapture(GL11.GL_FRONT)) {
            renderWithoutSwap.run();
            try (ScreenCapture loading = new ScreenCapture(GL11.GL_BACK)) {
                long start = System.nanoTime();
                double alpha;
                do {
                    alpha = Fade.in((System.nanoTime() - start) / 1_000_000.0, duration);
                    previous.draw(1);
                    loading.draw(alpha);
                    Display.update(false);
                    Display.processMessages();
                    if (alpha < 1) Display.sync(60);
                } while (alpha < 1 && Display.isCreated() && !Display.isCloseRequested());
            }
        }
    }

    public void finish(int duration) {
        close();
        if (duration == 0) return;
        exit = new ScreenCapture(GL11.GL_FRONT);
        exitDuration = duration;
        exitStart = -1;
    }

    public void renderExit() {
        if (exit == null) return;
        long now = System.nanoTime();
        if (exitStart == -1) exitStart = now;
        double alpha = Fade.out((now - exitStart) / 1_000_000.0, exitDuration);
        if (alpha <= 0) close();
        else exit.draw(alpha);
    }

    @Override
    public void close() {
        if (exit != null) {
            exit.close();
            exit = null;
        }
    }
}
