package com.legacyvisualfix.smoke;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.wdmla.api.ui.IDrawable;
import com.gtnewhorizons.wdmla.api.ui.sizer.IArea;
import com.gtnewhorizons.wdmla.impl.ui.component.RootComponent;
import com.gtnewhorizons.wdmla.impl.ui.component.TextComponent;
import com.gtnewhorizons.wdmla.impl.ui.sizer.Size;
import com.gtnewhorizons.wdmla.impl.ui.value.HUDRenderArea;
import com.legacyvisualfix.waila.WailaAnimationConfig;
import com.legacyvisualfix.waila.WdmlaAnimationRenderer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import mcp.mobius.waila.overlay.OverlayConfig;

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
                WdmlaAnimationRenderer.beginFrame();
                WdmlaAnimationRenderer.endFrame();
                render(large);
                require(close(rendered.getW(), large.getWidth() + 10), "hidden reset");
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
                finish(null);
            } catch (Throwable failure) {
                finish(failure);
            }
        }

        private void advance(int milliseconds) throws Exception {
            Field animation = WdmlaAnimationRenderer.class.getDeclaredField("ANIMATION");
            animation.setAccessible(true);
            Object state = animation.get(null);
            Field started = state.getClass()
                .getDeclaredField("started");
            started.setAccessible(true);
            started.setLong(state, System.nanoTime() - milliseconds * 1_000_000L);
        }

        private RootComponent root(String text) throws Exception {
            RootComponent root = new RootComponent();
            root.child(new TextComponent(text));
            Field field = RootComponent.class.getDeclaredField("background");
            field.setAccessible(true);
            IDrawable original = (IDrawable) field.get(root);
            field.set(root, (IDrawable) area -> {
                rendered = area;
                original.draw(area);
            });
            return root;
        }

        private void render(RootComponent root) {
            float width = root.getWidth(), height = root.getHeight();
            boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            int depth = GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH);
            root.renderHUD();
            require(width == root.getWidth() && height == root.getHeight(), "component layout untouched");
            require(scissor == GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), "scissor restored");
            require(depth == GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH), "attribute stack restored");
            IArea expected = new HUDRenderArea(new Size(rendered.getW() - 10, rendered.getH() - 10))
                .computeBackground();
            require(close(expected.getX(), rendered.getX()), "horizontal anchor");
            require(close(expected.getY(), rendered.getY()), "vertical anchor");
        }

        private void finish(Throwable failure) {
            finished = true;
            String result = failure == null
                ? "PASS: WDMla transformed renderer, grow/shrink, layout, anchors, hidden reset, disabled/zero duration, scale reset, GL restoration"
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
