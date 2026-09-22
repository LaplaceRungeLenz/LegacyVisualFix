package com.legacyvisualfix.smoke;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.config.Configuration;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.wdmla.api.ui.IDrawable;
import com.gtnewhorizons.wdmla.api.ui.sizer.IArea;
import com.gtnewhorizons.wdmla.impl.ui.component.HPanelComponent;
import com.gtnewhorizons.wdmla.impl.ui.component.ItemComponent;
import com.gtnewhorizons.wdmla.impl.ui.component.RootComponent;
import com.gtnewhorizons.wdmla.impl.ui.component.TextComponent;
import com.gtnewhorizons.wdmla.impl.ui.sizer.Size;
import com.gtnewhorizons.wdmla.impl.ui.value.HUDRenderArea;
import com.gtnewhorizons.wdmla.overlay.GuiBlockDraw;
import com.gtnewhorizons.wdmla.overlay.WDMlaTickHandler;
import com.legacyvisualfix.waila.WailaAnimationConfig;
import com.legacyvisualfix.waila.WdmlaAnimationRenderer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import mcp.mobius.waila.api.impl.ConfigHandler;
import mcp.mobius.waila.overlay.OverlayConfig;
import mcp.mobius.waila.utils.Constants;

/** Opt-in test of actual WDMla mixin transformation and OpenGL rendering. */
@Mod(
    modid = "legacyvisualfixwdmlasmoke",
    version = "1",
    dependencies = "required-after:legacyvisualfix;required-after:wdmla")
public final class WdmlaSmoke {

    private int ticks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ++ticks == 40) {
            Minecraft.getMinecraft()
                .displayGuiScreen(new TestScreen());
        }
    }

    private static final class TestScreen extends GuiScreen {

        private RootComponent small, large;
        private RootComponent activeRoot;
        private IArea rendered;
        private boolean finished;

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            if (finished) return;
            try {
                mc.playerController = new PlayerControllerMP(mc, null);
                WailaAnimationConfig.enabled = true;
                WailaAnimationConfig.durationMs = 500;
                small = root("Short");
                large = root("A much longer WDMla tooltip with multiple components");
                large.child(new TextComponent("Second row"));
                render(small);
                require(close(rendered.getW(), small.getWidth() + 10), "initial size");
                for (RootComponent target : new RootComponent[] { large, small }) {
                    render(target);
                    advance(250);
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    GL11.glScissor(7, 11, 300, 200);
                    render(target);
                    IntBuffer box = BufferUtils.createIntBuffer(16);
                    GL11.glGetInteger(GL11.GL_SCISSOR_BOX, box);
                    require(
                        box.get(0) == 7 && box.get(1) == 11 && box.get(2) == 300 && box.get(3) == 200,
                        "existing scissor rectangle restored");
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                    require(rendered.getW() > small.getWidth() + 10, "transition lower bound");
                    require(rendered.getW() < large.getWidth() + 10, "transition upper bound");
                    advance(600);
                    render(target);
                    require(close(rendered.getW(), target.getWidth() + 10), "settled width");
                    require(close(rendered.getH(), target.getHeight() + 10), "settled height");
                }
                // WDMla leaves mainHUD null while awaiting a new target's server data.
                // Exercise the real event hook, including several empty frames, in both directions.
                for (RootComponent target : new RootComponent[] { large, small }) {
                    float previousWidth = rendered.getW();
                    idle(250);
                    for (int i = 0; i < 5; i++) overlay(null);
                    render(target);
                    require(close(rendered.getW(), previousWidth), "short target-data gap must preserve size");
                    advance(250);
                    render(target);
                    require(rendered.getW() > small.getWidth() + 10, "gap transition lower bound");
                    require(rendered.getW() < large.getWidth() + 10, "gap transition upper bound");
                    advance(600);
                    render(target);
                }
                idle(600);
                overlay(null);
                render(large);
                require(close(rendered.getW(), large.getWidth() + 10), "long hidden reset");
                idle(600);
                render(small);
                require(close(rendered.getW(), small.getWidth() + 10), "render pause reset");
                mc.gameSettings.hideGUI = true;
                try {
                    overlay(null);
                } finally {
                    mc.gameSettings.hideGUI = false;
                }
                render(large);
                require(close(rendered.getW(), large.getWidth() + 10), "explicit HUD hide reset");
                WailaAnimationConfig.enabled = false;
                render(small);
                require(close(rendered.getW(), small.getWidth() + 10), "disabled size");
                WailaAnimationConfig.enabled = true;
                WailaAnimationConfig.durationMs = 0;
                render(large);
                require(close(rendered.getW(), large.getWidth() + 10), "zero duration");
                WailaAnimationConfig.durationMs = 500;
                OverlayConfig.scale = 1.5f;
                render(small);
                require(close(rendered.getW(), small.getWidth() + 10), "scale reset");
                for (float scale : new float[] { 0.75f, 1.5f }) {
                    for (int anchor : new int[] { 0, 10000 }) {
                        OverlayConfig.scale = scale;
                        ConfigHandler.instance()
                            .setConfig(Configuration.CATEGORY_GENERAL, Constants.CFG_WAILA_POSX, anchor);
                        ConfigHandler.instance()
                            .setConfig(Configuration.CATEGORY_GENERAL, Constants.CFG_WAILA_POSY, anchor);
                        render(small);
                        render(large);
                        advance(250);
                        GL11.glClearColor(0.08f, 0.10f, 0.13f, 1);
                        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                        render(large);
                        ScreenShotHelper.saveScreenshot(
                            mc.mcDataDir,
                            "wdmla-content-" + scale + "-" + anchor + ".png",
                            mc.displayWidth,
                            mc.displayHeight,
                            mc.getFramebuffer());
                        advance(600);
                        render(large);
                    }
                }
                WdmlaPendingProbe.run(this::overlay);
                finish(null);
            } catch (Throwable failure) {
                finish(failure);
            }
        }

        private void advance(int milliseconds) throws Exception {
            ageAnimationField("started", milliseconds);
        }

        private void idle(int milliseconds) throws Exception {
            ageAnimationField("lastUpdated", milliseconds);
        }

        private void ageAnimationField(String name, int milliseconds) throws Exception {
            Field animation = WdmlaAnimationRenderer.class.getDeclaredField("ANIMATION");
            animation.setAccessible(true);
            Object state = animation.get(null);
            Field started = state.getClass()
                .getDeclaredField(name);
            started.setAccessible(true);
            started.setLong(state, System.nanoTime() - milliseconds * 1_000_000L);
        }

        private RootComponent root(String text) throws Exception {
            RootComponent root = new RootComponent();
            HPanelComponent header = new HPanelComponent();
            header.child(new ItemComponent(new ItemStack(Blocks.stone)));
            header.child(new TextComponent(text) {

                @Override
                public void tick(float x, float y) {
                    FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
                    GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
                    float expected = Math.min(
                        1,
                        Math.min(
                            (rendered.getW() - 10) / activeRoot.getWidth(),
                            (rendered.getH() - 10) / activeRoot.getHeight()));
                    require(
                        close(matrix.get(0) / OverlayConfig.scale, expected),
                        "complete text must fit the animated box from the first frame");
                    probeViewport(x, y, expected);
                    super.tick(x, y);
                }
            });
            root.child(header);
            Field field = RootComponent.class.getDeclaredField("background");
            field.setAccessible(true);
            IDrawable original = (IDrawable) field.get(root);
            field.set(root, (IDrawable) area -> {
                rendered = area;
                original.draw(area);
            });
            return root;
        }

        private void probeViewport(float x, float y, float scale) {
            double pixels = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor()
                * OverlayConfig.scale;
            int rawX = (int) (x * pixels), rawY = mc.displayHeight - (int) ((y + 16) * pixels);
            int rawSize = (int) (16 * pixels);
            double pivotX = (rendered.getX() + 5) * pixels;
            double pivotY = mc.displayHeight - (rendered.getY() + 5) * pixels;
            int expectedX = (int) Math.round(pivotX + (rawX - pivotX) * scale);
            int expectedY = (int) Math.round(pivotY + (rawY - pivotY) * scale);
            int expectedSize = Math.round(rawSize * scale);
            try {
                GuiBlockDraw probe = new GuiBlockDraw() {

                    @Override
                    protected void drawWorld() {
                        IntBuffer viewport = BufferUtils.createIntBuffer(16);
                        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
                        require(
                            viewport.get(0) == expectedX && viewport.get(1) == expectedY
                                && viewport.get(2) == expectedSize
                                && viewport.get(3) == expectedSize,
                            "3D model viewport must follow the same scale and anchor as text");
                        if (WailaAnimationConfig.enabled && WailaAnimationConfig.durationMs > 0) {
                            require(GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), "model must not disable caller clipping");
                        }
                    }
                };
                Method render = GuiBlockDraw.class
                    .getDeclaredMethod("render", int.class, int.class, int.class, int.class);
                render.setAccessible(true);
                render.invoke(probe, rawX, rawY, rawSize, rawSize);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void overlay(RootComponent root) throws Exception {
            Field hud = WDMlaTickHandler.class.getDeclaredField("mainHUD");
            hud.setAccessible(true);
            hud.set(null, root);
            GuiScreen screen = mc.currentScreen;
            mc.currentScreen = null;
            try {
                RenderGameOverlayEvent parent = new RenderGameOverlayEvent(
                    0,
                    new ScaledResolution(mc, mc.displayWidth, mc.displayHeight),
                    0,
                    0);
                new WDMlaTickHandler()
                    .overlayRender(new RenderGameOverlayEvent.Post(parent, RenderGameOverlayEvent.ElementType.ALL));
            } finally {
                mc.currentScreen = screen;
            }
        }

        private void render(RootComponent root) throws Exception {
            activeRoot = root;
            float width = root.getWidth(), height = root.getHeight();
            boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            int depth = GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH);
            int matrices = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
            IntBuffer viewportBefore = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBefore);
            overlay(root);
            require(width == root.getWidth() && height == root.getHeight(), "component layout untouched");
            require(scissor == GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), "scissor restored");
            require(depth == GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH), "attribute stack restored");
            require(matrices == GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH), "foreground matrix stack restored");
            IntBuffer viewportAfter = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewportAfter);
            for (int i = 0; i < 4; i++) require(viewportBefore.get(i) == viewportAfter.get(i), "viewport restored");
            IArea expected = new HUDRenderArea(new Size(rendered.getW() - 10, rendered.getH() - 10))
                .computeBackground();
            require(close(expected.getX(), rendered.getX()), "horizontal anchor");
            require(close(expected.getY(), rendered.getY()), "vertical anchor");
        }

        private void finish(Throwable failure) {
            finished = true;
            String result = failure == null
                ? "PASS: pending-data HUD continuity on all axes, response replacement, timeout/hide/disable resets, synchronized item/text/3D viewport transforms, anchors, scales, GL restoration"
                : "FAIL: " + failure;
            if (failure != null) failure.printStackTrace();
            try {
                Files.write(
                    new File(mc.mcDataDir, "legacyvisualfix-wdmla-smoke.txt").toPath(),
                    result.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println("LEGACYVISUALFIX_WDMLA_SMOKE " + result);
            mc.shutdown();
        }
    }

    private static boolean close(float a, float b) {
        return Math.abs(a - b) < 0.01f;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
