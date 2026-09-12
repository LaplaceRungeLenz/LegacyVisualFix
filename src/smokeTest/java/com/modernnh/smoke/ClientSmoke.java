package com.modernnh.smoke;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.ResourcePackRepository;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.modernnh.reload.ReloadScreen;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in actual client test. Never compiled into the normal mod artifact. */
@Mod(modid = "modernnhsmoke", name = "ModernNH smoke test", version = "1", dependencies = "required-after:modernnh")
public class ClientSmoke {

    private int ticks;
    private boolean running;
    private boolean waitingForFade;
    private int exitTicks;
    private boolean insideTestReload;
    private int reloads;
    private boolean throwFromListener;
    private final RuntimeException expectedFailure = new RuntimeException("Intentional smoke listener failure");
    private final AtomicInteger listeners = new AtomicInteger();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && waitingForFade) {
            if (++exitTicks < 30) {
                if (exitTicks <= 8) {
                    try {
                        screenshot(
                            new File(
                                Minecraft.getMinecraft().mcDataDir,
                                "modernnh-smoke/fade-frame-" + exitTicks + ".png"));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            File results = new File(mc.mcDataDir, "modernnh-smoke");
            try {
                java.lang.reflect.Field transitions = ReloadScreen.class.getDeclaredField("TRANSITIONS");
                transitions.setAccessible(true);
                Object value = transitions.get(null);
                java.lang.reflect.Field exit = value.getClass()
                    .getDeclaredField("exit");
                exit.setAccessible(true);
                if (exit.get(value) != null) throw new AssertionError("Fade-out did not finish in normal game frames");
                screenshot(new File(results, "fade-finished.png"));
                Files.write(
                    new File(results, "result.txt").toPath(),
                    "PASS: resources, configuration, GL state, exception recovery, optional shader tests and real-frame fade-out cleanup\n"
                        .getBytes(StandardCharsets.UTF_8));
                System.out.println("MODERNNH_SMOKE_PASS");
            } catch (Throwable e) {
                e.printStackTrace();
                try {
                    Files.write(
                        new File(results, "result.txt").toPath(),
                        ("FAIL: " + e).getBytes(StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            } finally {
                mc.shutdown();
            }
            return;
        }
        if (event.phase != TickEvent.Phase.END || running || ++ticks < 30) return;
        running = true;
        Minecraft mc = Minecraft.getMinecraft();
        File results = new File(mc.mcDataDir, "modernnh-smoke");
        results.mkdirs();
        try {
            File themeFile = new File(mc.mcDataDir, "config/modernnh/theme.properties");
            try (java.io.InputStream defaults = getClass()
                .getResourceAsStream("/assets/modernnh/theme-0.1.properties")) {
                Files.copy(defaults, themeFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            ((IReloadableResourceManager) mc.getResourceManager())
                .registerReloadListener(new IResourceManagerReloadListener() {

                    @Override
                    public void onResourceManagerReload(IResourceManager manager) {
                        if (!insideTestReload) return;
                        if (throwFromListener) throw expectedFailure;
                        listeners.incrementAndGet();
                        try {
                            Thread.sleep(65);
                            int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
                            int matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
                            int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                            int[] viewport = new int[4];
                            java.nio.IntBuffer vp = BufferUtils.createIntBuffer(16);
                            GL11.glGetInteger(GL11.GL_VIEWPORT, vp);
                            vp.get(viewport);
                            ReloadScreen.textureProgress();
                            java.lang.reflect.Field failed = ReloadScreen.class.getDeclaredField("failed");
                            failed.setAccessible(true);
                            if (failed.getBoolean(null)) throw new AssertionError("Renderer disabled by an error");
                            if (active != GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
                                || matrix != GL11.glGetInteger(GL11.GL_MATRIX_MODE)
                                || binding != GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)) {
                                throw new AssertionError("GL state changed by reload rendering");
                            }
                            vp.clear();
                            GL11.glGetInteger(GL11.GL_VIEWPORT, vp);
                            for (int i = 0; i < 4; i++) {
                                if (vp.get(i) != viewport[i]) throw new AssertionError("Viewport not restored");
                            }
                            screenshot(new File(results, "reload-" + reloads + ".png"));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            for (reloads = 0; reloads < 5; reloads++) {
                if (reloads == 1) {
                    try (java.io.InputStream logo = getClass()
                        .getResourceAsStream("/assets/modernnh/textures/gui/logo.png")) {
                        Files.copy(
                            logo,
                            new File(themeFile.getParentFile(), "custom-logo.png").toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    mc.gameSettings.language = "zh_CN";
                    for (Language language : mc.getLanguageManager()
                        .getLanguages()) {
                        if ("zh_CN".equals(language.getLanguageCode())) mc.getLanguageManager()
                            .setCurrentLanguage(language);
                    }
                    File theme = new File(mc.mcDataDir, "config/modernnh/theme.properties");
                    Files.write(
                        theme.toPath(),
                        ("bar.width=280\ncolor.fill=FFFFBC52\nbackground.fit=contain\ntexture.logo=file:custom-logo.png\nshowDetails=true\n")
                            .getBytes(StandardCharsets.ISO_8859_1));
                }
                if (reloads == 2) {
                    File theme = new File(mc.mcDataDir, "config/modernnh/theme.properties");
                    Files.write(
                        theme.toPath(),
                        ("texture.background=file:missing.png\ntexture.logo=file:missing.png\ntexture.track=\ntexture.fill=\nbar.width=-1\nbar.x=NaN\n")
                            .getBytes(StandardCharsets.ISO_8859_1));
                }
                if (reloads == 3) {
                    File pack = new File(mc.mcDataDir, "resourcepacks/ModernNH-Smoke");
                    File gui = new File(pack, "assets/modernnh/textures/gui");
                    gui.mkdirs();
                    Files.write(
                        new File(pack, "pack.mcmeta").toPath(),
                        "{\"pack\":{\"pack_format\":1,\"description\":\"ModernNH smoke pack\"}}"
                            .getBytes(StandardCharsets.UTF_8));
                    BufferedImage bg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g = bg.createGraphics();
                    g.setColor(new java.awt.Color(38, 18, 48));
                    g.fillRect(0, 0, 16, 16);
                    g.dispose();
                    ImageIO.write(bg, "png", new File(gui, "background.png"));
                    Files.write(themeFile.toPath(), "bar.x=0.5\n".getBytes(StandardCharsets.ISO_8859_1));
                    ResourcePackRepository repository = mc.getResourcePackRepository();
                    repository.updateRepositoryEntriesAll();
                    for (ResourcePackRepository.Entry entry : repository.getRepositoryEntriesAll()) {
                        if ("ModernNH-Smoke".equals(entry.getResourcePackName())) {
                            repository.func_148527_a(Collections.singletonList(entry));
                        }
                    }
                }
                if (reloads == 4) mc.getResourcePackRepository()
                    .func_148527_a(Collections.emptyList());
                insideTestReload = true;
                mc.refreshResources();
                insideTestReload = false;
                if (reloads == 0) {
                    java.util.Properties migrated = new java.util.Properties();
                    try (java.io.InputStream input = Files.newInputStream(themeFile.toPath())) {
                        migrated.load(input);
                    }
                    if (!"0.4".equals(migrated.getProperty("bar.widthFraction"))
                        || !new File(themeFile.getParentFile(), "theme-0.1.properties.bak").exists()) {
                        throw new AssertionError("Stock theme migration or backup failed");
                    }
                }
                Thread.sleep(1000);
            }
            if (listeners.get() != 5) throw new AssertionError("Reload listener count: " + listeners.get());
            insideTestReload = true;
            throwFromListener = true;
            try {
                mc.refreshResources();
                throw new AssertionError("Reload exception was swallowed");
            } catch (RuntimeException expected) {
                if (expected != expectedFailure) throw expected;
            }
            java.lang.reflect.Field progressField = ReloadScreen.class.getDeclaredField("PROGRESS");
            progressField.setAccessible(true);
            if (((com.modernnh.reload.ReloadProgress) progressField.get(null)).isActive()) {
                throw new AssertionError("Failed reload left screen active");
            }
            throwFromListener = false;
            Thread.sleep(1000);
            mc.refreshResources();
            insideTestReload = false;
            Thread.sleep(1000);
            if (listeners.get() != 6) throw new AssertionError("Reload did not recover");
            CaptureSmoke.run();
            ShaderSmoke.run(mc, results);
            waitingForFade = true;
            Files.write(
                new File(results, "result.txt").toPath(),
                "PENDING: reload tests complete; waiting for actual rendered fade-out frames\n"
                    .getBytes(StandardCharsets.UTF_8));
        } catch (Throwable e) {
            e.printStackTrace();
            try {
                Files.write(new File(results, "result.txt").toPath(), ("FAIL: " + e).getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
        } finally {
            if (!waitingForFade) mc.shutdown();
        }
    }

    static void screenshot(File output) throws Exception {
        int width = Display.getWidth();
        int height = Display.getHeight();
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        int fbo = GL11.glGetInteger(0x8CA6);
        int oldReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        net.minecraft.client.renderer.OpenGlHelper.func_153171_g(0x8D40, 0);
        GL11.glReadBuffer(GL11.GL_FRONT);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        net.minecraft.client.renderer.OpenGlHelper.func_153171_g(0x8D40, fbo);
        GL11.glReadBuffer(oldReadBuffer);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (y * width + x) * 4;
                image.setRGB(
                    x,
                    height - y - 1,
                    ((buffer.get(i) & 255) << 16) | ((buffer.get(i + 1) & 255) << 8) | (buffer.get(i + 2) & 255));
            }
        }
        ImageIO.write(image, "png", output);
    }
}
