package com.modernnh.reload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Properties;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.Display;

import com.modernnh.ModernNH;
import com.modernnh.render.LoadingRenderer;
import com.modernnh.render.Transitions;
import com.modernnh.theme.Theme;
import com.modernnh.theme.ThemeMigration;
import com.modernnh.theme.ThemeTextures;

import cpw.mods.fml.common.ProgressManager;

/** Only invoked by client Mixins. No worker thread ever touches the display or resource listeners. */
public final class ReloadScreen {

    private static final ReloadProgress PROGRESS = new ReloadProgress();
    private static final ReloadScope SCOPE = new ReloadScope();
    private static final LoadingRenderer RENDERER = new LoadingRenderer();
    private static final Transitions TRANSITIONS = new Transitions();
    private static boolean shaderSession;
    private static int shaderSessions;
    private static Minecraft minecraft;
    private static Thread clientThread;
    private static File directory;
    private static Theme theme = Theme.parse(new Properties());
    private static ThemeTextures textures;
    private static boolean drawing;
    private static boolean failed;
    private static boolean resourceStateInvalid;
    private static long lastFrame;
    private static String detail = "";

    private ReloadScreen() {}

    public static void initialize(Minecraft mc) {
        minecraft = mc;
        clientThread = Thread.currentThread();
        directory = new File(mc.mcDataDir, "config/modernnh");
        reloadTheme();
        ModernNH.LOG.info("ModernNH runtime reload screen ready");
    }

    public static void runReload(int listeners, Runnable original) {
        runSession(listeners, original, false);
    }

    public static void runShaderReload(Runnable original) {
        runSession(3, original, true);
    }

    public static void runShaderPipeline(Runnable original) {
        if (shaderSession && PROGRESS.isActive()) original.run();
        else runSession(1, original, true);
    }

    public static void shaderStep(String stage) {
        if (!shaderSession || !observing()) return;
        PROGRESS.afterListener();
        PROGRESS.beforeListener(stage);
        draw(true);
    }

    public static void shaderPulse() {
        if (shaderSession && observing()) draw(false);
    }

    private static void runSession(int listeners, Runnable original, boolean shaders) {
        if (minecraft == null || Thread.currentThread() != clientThread || !Display.isCreated()) {
            original.run();
            return;
        }
        boolean owner = SCOPE.enter();
        boolean success = false;
        try {
            if (owner) {
                shaderSession = shaders;
                if (shaders) shaderSessions++;
                failed = false;
                // The previous manager may still contain a failed pack during vanilla's automatic retry.
                if (!resourceStateInvalid) reloadTheme();
                if (!shaders) resourceStateInvalid = true;
                PROGRESS.begin(listeners);
                detail = shaders ? "Preparing shaders" : "Preparing resource packs";
                lastFrame = 0;
                enter();
            }
            original.run();
            success = true;
        } finally {
            if (SCOPE.exit()) {
                if (success) {
                    if (shaderSession) while (PROGRESS.getCompleted() < PROGRESS.getTotal()) PROGRESS.afterListener();
                    detail = "Reload complete";
                    draw(true);
                }
                try {
                    if (theme.enabled && !failed) TRANSITIONS.finish(theme.fadeOutMs);
                } catch (RuntimeException | LinkageError e) {
                    ModernNH.LOG.warn("Cannot start reload fade-out", e);
                }
                PROGRESS.finish();
                // A broken new resource pack must not replace the last valid cache.
                if (success && !shaderSession) {
                    resourceStateInvalid = false;
                    reloadTheme();
                }
            }
        }
    }

    public static void beforeListener(Object listener) {
        if (!observing()) return;
        String name = listener.getClass()
            .getSimpleName();
        PROGRESS.beforeListener(
            name.isEmpty() ? listener.getClass()
                .getName() : name);
        detail = "";
        draw(false);
    }

    public static void afterListener() {
        if (!observing()) return;
        PROGRESS.afterListener();
        detail = "";
        draw(false);
    }

    @SuppressWarnings("deprecation")
    public static void textureProgress() {
        if (!observing()) return;
        Iterator<ProgressManager.ProgressBar> bars = ProgressManager.barIterator();
        ProgressManager.ProgressBar latest = null;
        while (bars.hasNext()) latest = bars.next();
        if (latest != null) {
            // Forge increments before doing the work: label as current item, never completed fraction.
            detail = latest.getTitle() + "  item "
                + latest.getStep()
                + " / "
                + latest.getSteps()
                + "  "
                + latest.getMessage();
        }
        draw(false);
    }

    private static boolean observing() {
        return Thread.currentThread() == clientThread && SCOPE.isOutermost() && PROGRESS.isActive() && !drawing;
    }

    private static void draw(boolean force) {
        if (!PROGRESS.isActive() || !theme.enabled || failed || drawing || !Display.isCreated()) return;
        long now = System.nanoTime();
        if (!force && now - lastFrame < 50_000_000L) return;
        drawing = true;
        try {
            RENDERER.draw(
                minecraft,
                theme,
                textures,
                PROGRESS,
                detail,
                shaderSession ? "Reloading shaders" : "Reloading resources",
                true);
            lastFrame = System.nanoTime();
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            ModernNH.LOG.error("Custom reload rendering failed; disabling it for this reload", e);
        } finally {
            drawing = false;
        }
    }

    private static void enter() {
        try {
            TRANSITIONS.close();
            if (!theme.enabled) return;
            drawing = true;
            TRANSITIONS.enter(
                theme.fadeInMs,
                () -> RENDERER.draw(
                    minecraft,
                    theme,
                    textures,
                    PROGRESS,
                    detail,
                    shaderSession ? "Reloading shaders" : "Reloading resources",
                    false));
        } catch (RuntimeException | LinkageError e) {
            ModernNH.LOG.warn("Cannot animate reload entry; using normal loading screen", e);
        } finally {
            drawing = false;
        }
        draw(true);
    }

    /** Invoked after the game draws its new frame, before the normal display swap. */
    public static void renderTransition() {
        if (minecraft == null || Thread.currentThread() != clientThread || PROGRESS.isActive() || !Display.isCreated())
            return;
        try {
            TRANSITIONS.renderExit();
        } catch (RuntimeException | LinkageError e) {
            TRANSITIONS.close();
            ModernNH.LOG.warn("Cannot animate reload exit", e);
        }
    }

    private static void reloadTheme() {
        try {
            Files.createDirectories(directory.toPath());
            File config = new File(directory, "theme.properties");
            if (!config.exists()) {
                try (InputStream defaults = ReloadScreen.class
                    .getResourceAsStream("/assets/modernnh/theme.properties")) {
                    if (defaults == null) throw new IOException("Default theme configuration missing");
                    Files.copy(defaults, config.toPath());
                }
            }
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(config.toPath())) {
                properties.load(input);
            }
            for (String version : new String[] { "0.1", "0.2" }) {
                Properties previous = new Properties();
                Properties next = new Properties();
                try (
                    InputStream oldDefaults = ReloadScreen.class
                        .getResourceAsStream("/assets/modernnh/theme-" + version + ".properties");
                    InputStream newDefaults = ReloadScreen.class
                        .getResourceAsStream("/assets/modernnh/theme.properties")) {
                    if (oldDefaults != null && newDefaults != null) {
                        previous.load(oldDefaults);
                        next.load(newDefaults);
                    }
                }
                if (ThemeMigration.upgrade(properties, previous, next)) {
                    File backup = new File(directory, "theme-" + version + ".properties.bak");
                    if (!backup.exists()) Files.copy(config.toPath(), backup.toPath());
                    try (java.io.OutputStream output = Files.newOutputStream(config.toPath())) {
                        properties.store(output, "ModernNH 0.3 theme; original defaults backed up alongside this file");
                    }
                }
            }
            Theme replacementTheme = Theme.parse(properties);
            ThemeTextures replacement = replacementTheme.enabled
                ? new ThemeTextures(minecraft, directory, replacementTheme)
                : null;
            ThemeTextures old = textures;
            textures = replacement;
            theme = replacementTheme;
            if (old != null) old.close();
        } catch (IOException | RuntimeException | LinkageError e) {
            ModernNH.LOG.warn("Cannot update reload theme; retaining previous theme", e);
        }
    }

    public static void shutdown() {
        if (Thread.currentThread() != clientThread || !Display.isCreated()) return;
        try {
            RENDERER.close();
            TRANSITIONS.close();
            if (textures != null) textures.close();
        } catch (RuntimeException | LinkageError e) {
            ModernNH.LOG.warn("Cannot release reload textures during shutdown", e);
        } finally {
            textures = null;
            minecraft = null;
            PROGRESS.finish();
        }
    }
}
