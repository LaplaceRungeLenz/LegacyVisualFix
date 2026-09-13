package com.modernnh.smoke;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.modernnh.mixin.waila.AccessorTooltip;
import com.modernnh.waila.TooltipAnimation;
import com.modernnh.waila.WailaAnimationConfig;
import com.modernnh.waila.WailaAnimationRenderer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import mcp.mobius.waila.api.IWailaCommonAccessor;
import mcp.mobius.waila.api.IWailaTooltipRenderer;
import mcp.mobius.waila.api.SpecialChars;
import mcp.mobius.waila.api.impl.ModuleRegistrar;
import mcp.mobius.waila.overlay.OverlayConfig;
import mcp.mobius.waila.overlay.OverlayRenderer;
import mcp.mobius.waila.overlay.Tooltip;

/** Opt-in real OpenGL test with synthetic Waila content. Never included in normal builds. */
@Mod(
    modid = "modernnhwailasmoke",
    name = "ModernNH Waila smoke",
    version = "1",
    dependencies = "required-after:modernnh;required-after:Waila")
public final class WailaSmoke {

    private int ticks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ++ticks == 40) Minecraft.getMinecraft()
            .displayGuiScreen(new TestScreen());
    }

    private static final class TestScreen extends GuiScreen {

        private final boolean chromatic = Loader.isModLoaded("chromatictooltipscompat")
            && com.slprime.chromatictooltipscompat.CompatConfig.wailaEnabled;
        private final String[] packs = { "", "gregtech-tooltips-simple.zip", "gregtech-tooltips-icon.zip", "" };
        private int scenario, phase;
        private long phaseStart;
        private Tooltip small, large;
        private double smallWidth, largeWidth;
        private boolean intermediate;
        private WorldClient testWorld;
        private int customDraws;
        private TooltipAnimation animation;
        private final File results = new File(Minecraft.getMinecraft().mcDataDir, "modernnh-waila-smoke");
        private boolean finished;

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            if (finished) return;
            try {
                if (phase == 0) prepare();
                GL11.glClearColor(0.13f, 0.16f, 0.20f, 1);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                long elapsed = System.nanoTime() - phaseStart;
                Tooltip value = phase == 1 || phase == 3 ? small : large;
                render(value);
                if (phase == 1) {
                    smallWidth = animation.width();
                    capture("small");
                    nextPhase();
                } else if (phase == 2) {
                    if (elapsed > 50_000_000 && elapsed < 140_000_000) {
                        if (!intermediate && animation.width() > smallWidth) {
                            capture("growing");
                            intermediate = true;
                        }
                    }
                    if (elapsed > 230_000_000) {
                        require(customDraws > 0, "custom renderer was never drawn");
                        largeWidth = animation.width();
                        require(largeWidth > smallWidth, "size never changed");
                        require(intermediate, "no intermediate growing frame");
                        capture("large");
                        nextPhase();
                    }
                } else if (phase == 3 && elapsed > 230_000_000) {
                    require(Math.abs(animation.width() - smallWidth) < 0.01, "shrink did not finish");
                    // Hidden frames reset the animation, including missing targets/null tooltip.
                    OverlayRenderer.renderOverlay(null);
                    render(large);
                    require(Math.abs(animation.width() - largeWidth) < 0.01, "hidden overlay did not reset");
                    WailaAnimationConfig.enabled = false;
                    render(small);
                    WailaAnimationConfig.enabled = true;
                    if (chromatic) ChromaticSmoke.verifyInventoryIsolation(small);
                    if (++scenario < (chromatic ? packs.length : 3)) phase = 0;
                    else finish(null);
                }
            } catch (Throwable failure) {
                finish(failure);
            }
        }

        private void prepare() throws Exception {
            results.mkdirs();
            customDraws = 0;
            if (testWorld == null) testWorld = SyntheticTarget.createWorld();
            ModuleRegistrar.instance()
                .registerTooltipRenderer("modernnh.smoke", new IWailaTooltipRenderer() {

                    public Dimension getSize(String[] params, IWailaCommonAccessor accessor) {
                        return new Dimension(100, 8);
                    }

                    public void draw(String[] params, IWailaCommonAccessor accessor) {
                        customDraws++;
                        GuiScreen.drawRect(0, 0, 100, 7, 0xff334455);
                        GuiScreen.drawRect(0, 0, 47, 7, 0xffddbb55);
                    }
                });
            WailaAnimationConfig.enabled = true;
            WailaAnimationConfig.durationMs = 150;
            OverlayConfig.scale = chromatic ? 0.9f : new float[] { 1, 0.75f, 1.5f }[scenario];
            if (chromatic) applyPack(packs[scenario]);
            mc.gameSettings.guiScale = scenario % 2 + 1;
            small = new Tooltip(Arrays.asList("Stone", "Minecraft"), new ItemStack(Blocks.stone));
            large = new Tooltip(
                Arrays.asList(
                    "Large machine controller",
                    "Energy: 32768 / 65536 EU",
                    SpecialChars.getRenderString("modernnh.smoke"),
                    "GregTech synthetic smoke data"),
                new ItemStack(Blocks.furnace));
            ((AccessorTooltip) small).modernnh$getAnchor()
                .setLocation(5000, 1000);
            ((AccessorTooltip) large).modernnh$getAnchor()
                .setLocation(5000, 1000);
            Class<?> owner = chromatic ? Class.forName("com.modernnh.waila.chromatic.ChromaticAnimation")
                : WailaAnimationRenderer.class;
            Field field = owner.getDeclaredField("ANIMATION");
            field.setAccessible(true);
            animation = (TooltipAnimation) field.get(null);
            // Keep the old Chromatic context across resource reloads: the next tooltip must
            // select the new theme even without looking away first.
            animation.reset();
            intermediate = false;
            phase = 1;
            phaseStart = System.nanoTime();
        }

        private void render(Tooltip tooltip) throws Exception {
            AccessorTooltip fields = (AccessorTooltip) tooltip;
            int x = fields.modernnh$getX(), y = fields.modernnh$getY();
            int w = fields.modernnh$getWidth(), h = fields.modernnh$getHeight();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(3, 4, mc.displayWidth - 6, mc.displayHeight - 8);
            while (GL11.glGetError() != GL11.GL_NO_ERROR) {}
            try (SyntheticTarget target = new SyntheticTarget(testWorld)) {
                require(OverlayRenderer.isOverlayVisible(), "visible render entry rejected target");
                OverlayRenderer.renderOverlay(tooltip);
            }
            IntBuffer scissor = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, scissor);
            require(
                GL11.glIsEnabled(GL11.GL_SCISSOR_TEST) && scissor.get(0) == 3
                    && scissor.get(1) == 4
                    && scissor.get(2) == mc.displayWidth - 6
                    && scissor.get(3) == mc.displayHeight - 8,
                "scissor state leaked");
            require(GL11.glGetError() == GL11.GL_NO_ERROR, "new GL error");
            require(
                x == fields.modernnh$getX() && y == fields.modernnh$getY()
                    && w == fields.modernnh$getWidth()
                    && h == fields.modernnh$getHeight(),
                "tooltip layout mutated");
            if (chromatic) ChromaticSmoke.verifyTheme();
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }

        private void applyPack(String name) throws Exception {
            ResourcePackRepository repository = mc.getResourcePackRepository();
            repository.updateRepositoryEntriesAll();
            if (name.isEmpty()) repository.func_148527_a(Collections.emptyList());
            else {
                ResourcePackRepository.Entry selected = null;
                for (ResourcePackRepository.Entry entry : repository.getRepositoryEntriesAll()) {
                    if (name.equals(entry.getResourcePackName())) selected = entry;
                }
                require(selected != null, "missing test resource pack " + name);
                repository.func_148527_a(Collections.singletonList(selected));
            }
            mc.refreshResources();
        }

        private void nextPhase() {
            phase++;
            phaseStart = System.nanoTime();
        }

        private void capture(String suffix) throws Exception {
            ByteBuffer pixels = BufferUtils.createByteBuffer(mc.displayWidth * mc.displayHeight * 4);
            GL11.glReadPixels(0, 0, mc.displayWidth, mc.displayHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            BufferedImage image = new BufferedImage(mc.displayWidth, mc.displayHeight, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < mc.displayHeight; y++) for (int x = 0; x < mc.displayWidth; x++) {
                int i = (y * mc.displayWidth + x) * 4;
                image.setRGB(
                    x,
                    mc.displayHeight - y - 1,
                    0xff000000 | (pixels.get(i) & 255) << 16
                        | (pixels.get(i + 1) & 255) << 8
                        | (pixels.get(i + 2) & 255));
            }
            ImageIO.write(
                image,
                "png",
                new File(results, (chromatic ? "chromatic-" : "waila-") + scenario + "-" + suffix + ".png"));
        }

        private void finish(Throwable failure) {
            finished = true;
            String text = failure == null
                ? "PASS: actual transformed renderer, resize, hidden reset, disabled mode, scales, layout/scissor restoration"
                    + (chromatic ? ", default/simple/icon/removed resource packs, theme freshness, inventory isolation"
                        : "")
                : "FAIL: " + failure;
            if (failure != null) failure.printStackTrace();
            try {
                Files.write(
                    new File(results, chromatic ? "chromatic-result.txt" : "waila-result.txt").toPath(),
                    text.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("MODERNNH_WAILA_SMOKE " + text);
            mc.shutdown();
        }
    }

    static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
