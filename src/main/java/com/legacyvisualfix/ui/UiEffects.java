package com.legacyvisualfix.ui;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.inventory.InventoryAnimations;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** One render-thread session. Never changes the container, stack, input or framebuffer. */
public final class UiEffects {

    private static final Map<Slot, Double> SCALES = new IdentityHashMap<>();
    private static final TrailParticles TRAIL = new TrailParticles();
    private static final UiMotion.Spring ROTATION = new UiMotion.Spring();
    private static Object screen;
    private static int width, height, mouseX, mouseY, oldX, oldY;
    private static long lastFrame;
    private static double dt, time;
    private static boolean emitted, rendered, moving, hadCarried;
    private static int depth, matrixMode;
    private static boolean pushed;
    private static boolean nei, obscured;

    private UiEffects() {}

    public static void register() {
        nei = Loader.isModLoaded("NotEnoughItems");
        UiEffects events = new UiEffects();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance()
            .bus()
            .register(events);
    }

    @SubscribeEvent
    public void opened(GuiOpenEvent event) {
        if (event.gui != screen) clear();
    }

    @SubscribeEvent
    public void resized(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui == screen) clear();
    }

    @SubscribeEvent
    public void post(GuiScreenEvent.DrawScreenEvent.Post event) {
        renderParticles(event.gui);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getMinecraft().currentScreen != screen) clear();
    }

    public static void clear() {
        screen = null;
        lastFrame = 0;
        SCALES.clear();
        TRAIL.clear();
        ROTATION.reset();
        hadCarried = false;
    }

    public static boolean allowed(Object gui) {
        if (!UiEffectsConfig.enabled || gui == null || Minecraft.getMinecraft().thePlayer == null) return false;
        for (String excluded : UiEffectsConfig.excludedScreens) if (gui.getClass()
            .getName()
            .equals(excluded)) return false;
        return true;
    }

    public static void frame(Object gui, int x, int y) {
        if (!(gui instanceof GuiScreen) || !allowed(gui)) {
            clear();
            return;
        }
        GuiScreen g = (GuiScreen) gui;
        long now = System.nanoTime();
        if (gui == screen && lastFrame != 0 && now - lastFrame < 100_000) return;
        if (gui != screen || width != g.width || height != g.height) {
            clear();
            screen = gui;
            width = g.width;
            height = g.height;
            oldX = x;
            oldY = y;
        } else {
            oldX = mouseX;
            oldY = mouseY;
        }
        double elapsed = lastFrame == 0 ? 0 : Math.max(0, (now - lastFrame) * 1e-9);
        dt = Math.min(0.1, elapsed);
        lastFrame = now;
        mouseX = x;
        mouseY = y;
        obscured = nei && NeiUiBridge.obscured(gui, x, y);
        time += dt;
        emitted = false;
        rendered = false;
        moving = dt > 0 && elapsed <= 0.1 && (oldX != x || oldY != y);
        boolean carrying = Minecraft.getMinecraft().thePlayer.inventory.getItemStack() != null;
        if (carrying != hadCarried) {
            ROTATION.reset();
            oldX = x;
            oldY = y;
            moving = false;
        }
        hadCarried = carrying;
        double velocity = dt > 0 ? (x - oldX) / dt : 0;
        double target = carrying
            ? Math.max(-UiEffectsConfig.rotation, Math.min(UiEffectsConfig.rotation, velocity * 0.035))
            : 0;
        ROTATION.update(target, dt);
        TRAIL.update(elapsed);
        if (!UiEffectsConfig.trails) TRAIL.clear();
        // Remove abandoned slots after layouts change without retaining whole old containers.
        if (SCALES.size() > 1024) SCALES.clear();
    }

    public static boolean isMatching(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    public static void beginSlot(Object gui, Slot slot, double cx, double cy, boolean hovered) {
        if (!enter(gui) || slot == null || slot.getStack() == null || InventoryAnimations.entering(gui)) return;
        ItemStack held = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
        boolean match = isMatching(slot.getStack(), held);
        double target = UiEffectsConfig.hover && hovered && !obscured && (held == null || match)
            ? UiEffectsConfig.hoverScale
            : 1;
        double scale = UiMotion.approach(SCALES.getOrDefault(slot, 1d), target, UiEffectsConfig.speed, dt);
        if (Math.abs(scale - 1) < 0.0001 && target == 1) SCALES.remove(slot);
        else SCALES.put(slot, scale);
        double offset = UiEffectsConfig.matching && match
            ? Math.sin(time * 3.8 + slot.slotNumber * 0.73) * UiEffectsConfig.floatAmplitude
            : 0;
        if (Math.abs(scale - 1) < 0.0001 && Math.abs(offset) < 0.0001) return;
        push();
        GL11.glTranslated(cx, cy + offset, 0);
        GL11.glScaled(scale, scale, 1);
        GL11.glTranslated(-cx, -cy, 0);
    }

    public static void beginCarried(Object gui, ItemStack stack, int x, int y) {
        if (!enter(gui) || stack == null) return;
        ItemStack held = Minecraft.getMinecraft().thePlayer.inventory.getItemStack();
        if (!isMatching(stack, held)) return;
        if (!emitted) {
            emitted = true;
            if (UiEffectsConfig.trails && moving) {
                int color = rarityColor(stack);
                TRAIL.emit(
                    oldX,
                    oldY,
                    mouseX,
                    mouseY,
                    color,
                    dt,
                    UiEffectsConfig.maxParticles,
                    UiEffectsConfig.particleRate);
            }
        }
        if (!UiEffectsConfig.carried) return;
        push();
        GL11.glTranslated(x + 8, y + 8, 0);
        GL11.glRotated(ROTATION.value(), 0, 0, 1);
        GL11.glScaled(UiEffectsConfig.carriedScale, UiEffectsConfig.carriedScale, 1);
        GL11.glTranslated(-x - 8, -y - 8, 0);
    }

    private static int rarityColor(ItemStack stack) {
        if (stack.getRarity() == EnumRarity.common) return 0xd8dee9;
        String format = stack.getRarity().rarityColor.toString();
        int index = format.length() > 1 ? "0123456789abcdef".indexOf(format.charAt(1)) : -1;
        if (index < 0 || index > 15) return 0xffffff;
        int extra = (index >> 3 & 1) * 85;
        int r = (index >> 2 & 1) * 170 + extra, g = (index >> 1 & 1) * 170 + extra, b = (index & 1) * 170 + extra;
        if (index == 6) r += 85;
        return r << 16 | g << 8 | b;
    }

    private static boolean enter(Object gui) {
        return ++depth == 1 && gui == screen && allowed(gui);
    }

    private static void push() {
        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        pushed = true;
    }

    public static void endItem() {
        if (depth <= 0) return;
        if (--depth == 0 && pushed) {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(matrixMode);
            pushed = false;
        }
    }

    public static int particleCount() {
        return TRAIL.particles()
            .size();
    }

    /** MUI draws its foreground before the carried item: defer new particles until next frame. */
    public static void finishParticleLayer(Object gui) {
        renderParticles(gui);
        if (gui == screen) rendered = true;
    }

    public static void renderParticles(Object gui) {
        if (gui != screen || rendered
            || !allowed(gui)
            || TRAIL.particles()
                .isEmpty())
            return;
        rendered = true;
        // Called at screen-space Post (or before vanilla tooltip); no GUI-local transform is active.
        int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glBegin(GL11.GL_QUADS);
            for (TrailParticles.Particle p : TRAIL.particles()) {
                double fade = Math.max(0, 1 - p.age / p.lifetime), size = p.size * fade;
                GL11.glColor4d((p.rgb >> 16 & 255) / 255d, (p.rgb >> 8 & 255) / 255d, (p.rgb & 255) / 255d, fade * 0.8);
                double c = Math.cos(p.rotation) * size, s = Math.sin(p.rotation) * size;
                GL11.glVertex3d(p.x - c, p.y - s, 350);
                GL11.glVertex3d(p.x + s, p.y - c, 350);
                GL11.glVertex3d(p.x + c, p.y + s, 350);
                GL11.glVertex3d(p.x - s, p.y + c, 350);
            }
            GL11.glEnd();
        } finally {
            GL11.glPopMatrix();
            GL11.glMatrixMode(mode);
            GL11.glPopAttrib();
        }
    }
}
