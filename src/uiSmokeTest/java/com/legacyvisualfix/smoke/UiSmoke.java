package com.legacyvisualfix.smoke;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.ui.UiEffects;
import com.legacyvisualfix.ui.UiEffectsConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in integration test with real transformed container and Forge item renderer. */
@Mod(
    modid = "legacyvisualfixuismoke",
    name = "LegacyVisualFix UI smoke",
    version = "1",
    dependencies = "required-after:legacyvisualfix")
public final class UiSmoke {

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
            .displayGuiScreen(new Runner());
    }

    private static class Runner extends GuiScreen {

        boolean done;

        @Override
        public void drawScreen(int x, int y, float partial) {
            if (done) return;
            done = true;
            File dir = new File(mc.mcDataDir, "legacyvisualfix-ui-smoke");
            String result;
            try {
                dir.mkdirs();
                NetHandlerPlayClient handler = new NetHandlerPlayClient(mc, null, null) {

                    @Override
                    public void addToSendQueue(net.minecraft.network.Packet p) {}
                };
                WorldClient world = new WorldClient(
                    handler,
                    new WorldSettings(0, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT),
                    0,
                    EnumDifficulty.PEACEFUL,
                    mc.mcProfiler);
                mc.theWorld = world;
                mc.thePlayer = new EntityClientPlayerMP(mc, world, mc.getSession(), handler, new StatFileWriter());
                mc.thePlayer.movementInput = new net.minecraft.util.MovementInput();
                mc.playerController = new PlayerControllerMP(mc, handler);
                mc.renderViewEntity = mc.thePlayer;
                if (cpw.mods.fml.common.Loader.isModLoaded("NotEnoughItems")) {
                    Class<?> config = Class.forName("codechicken.nei.NEIClientConfig");
                    config.getMethod("loadWorld", String.class)
                        .invoke(null, "legacyvisualfix-ui-smoke");
                    config.getMethod("setEnabled", boolean.class)
                        .invoke(null, true);
                }
                Probe probe = new Probe();
                MinecraftForgeClient.registerItemRenderer(Items.diamond, probe);
                MinecraftForgeClient.registerItemRenderer(Items.nether_star, probe);
                Panel panel = new Panel();
                ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
                mc.currentScreen = panel;
                panel.setWorldAndResolution(mc, res.getScaledWidth(), res.getScaledHeight());
                mc.currentScreen = panel;
                UiEffectsConfig.enabled = false;
                while (GL11.glGetError() != 0) {}
                for (int i = 0; i < 20; i++) {
                    Thread.sleep(12);
                    panel.drawScreen(panel.left() + 28, panel.top() + 28, 0);
                }
                int baselineError = GL11.glGetError();
                System.out.println("LEGACYVISUALFIX_UI_GL disabledContainer=" + baselineError);
                UiEffectsConfig.enabled = true;
                float baseline = probe.scale;
                UiEffects.frame(panel, panel.left() + 28, panel.top() + 28);
                Thread.sleep(16);
                UiEffects.frame(panel, panel.left() + 28, panel.top() + 28);
                UiEffects.beginSlot(panel, panel.inventorySlots.getSlot(0), 28, 28, true);
                glCheck("isolated slot begin");
                UiEffects.endItem();
                glCheck("isolated slot end");
                for (int i = 0; i < 20; i++) {
                    Thread.sleep(12);
                    panel.drawScreen(panel.left() + 28, panel.top() + 28, 0);
                }
                require(
                    probe.scale > baseline * 1.08,
                    "hover scale did not reach item renderer: " + baseline + " -> " + probe.scale);
                int hoverError = GL11.glGetError();
                System.out.println("LEGACYVISUALFIX_UI_GL enabledHover=" + hoverError);
                require(hoverError == 0 || hoverError == baselineError, "new hover GL error " + hoverError);
                require(
                    panel.inventorySlots.getSlot(0)
                        .getStack().stackSize == 17,
                    "stack mutated");
                UiEffectsConfig.hover = false;
                for (int i = 0; i < 30; i++) {
                    Thread.sleep(12);
                    panel.drawScreen(-1000, -1000, 0);
                }
                float unmovedY = probe.posY;
                // Same-stack floating affects slot rendering even away from the cursor.
                mc.thePlayer.inventory.setItemStack(new ItemStack(Items.diamond, 3));
                probe.slotY = Float.NaN;
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(12);
                    panel.drawScreen(panel.left() + 100, panel.top() + 60, 0);
                }
                require(Math.abs(probe.slotY - unmovedY) > 0.01, "matching slot did not float");
                glCheck("matching", baselineError);
                // NBT/metadata are part of matching, count is not.
                ItemStack a = new ItemStack(Items.diamond, 1), b = new ItemStack(Items.diamond, 23);
                require(UiEffects.isMatching(a, b), "count should not affect match");
                b.setTagCompound(new NBTTagCompound());
                b.getTagCompound()
                    .setInteger("test", 1);
                require(!UiEffects.isMatching(a, b), "different NBT matched");
                require(
                    !UiEffects.isMatching(new ItemStack(Items.dye, 1, 1), new ItemStack(Items.dye, 1, 2)),
                    "metadata matched");
                UiEffects.clear();
                mc.thePlayer.inventory.setItemStack(new ItemStack(Items.diamond, 1));
                for (int i = 0; i < 12; i++) {
                    Thread.sleep(16);
                    panel.drawScreen(panel.left() + 35 + i * 5, panel.top() + 75, 0);
                }
                require(UiEffects.particleCount() > 0, "common item silver trail missing");
                java.lang.reflect.Field trailField = UiEffects.class.getDeclaredField("TRAIL");
                trailField.setAccessible(true);
                com.legacyvisualfix.ui.TrailParticles trail = (com.legacyvisualfix.ui.TrailParticles) trailField
                    .get(null);
                for (com.legacyvisualfix.ui.TrailParticles.Particle particle : trail.particles())
                    require(particle.rgb == 0xd8dee9, "common trail must be silver-white");
                UiEffects.clear();
                ItemStack rare = new ItemStack(Items.nether_star, 2);
                rare.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking, 1);
                mc.thePlayer.inventory.setItemStack(rare);
                for (int i = 0; i < 16; i++) {
                    Thread.sleep(16);
                    panel.drawScreen(panel.left() + 35 + i * 5, panel.top() + 75, 0);
                }
                require(probe.scale > 1.1 && Math.abs(probe.angle) > 0.5, "carried scaling/rotation missing");
                glCheck("carried and trail", baselineError);
                require(
                    UiEffects.particleCount() > 0 && UiEffects.particleCount() <= UiEffectsConfig.maxParticles,
                    "trail emission missing/unbounded");
                int oldParticles = UiEffects.particleCount();
                for (com.legacyvisualfix.ui.TrailParticles.Particle particle : trail.particles())
                    require(particle.rgb == 0x55ffff, "rare trail color changed");
                Thread.sleep(16);
                UiEffects.frame(panel, panel.left() + 110, panel.top() + 75);
                int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE),
                    stackDepth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
                int shade = GL11.glGetInteger(GL11.GL_SHADE_MODEL), blendSource = GL11.glGetInteger(GL11.GL_BLEND_SRC),
                    blendDestination = GL11.glGetInteger(GL11.GL_BLEND_DST);
                boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
                FloatBuffer color = BufferUtils.createFloatBuffer(16);
                GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
                glCheck("state queries before particle render");
                UiEffects.renderParticles(panel);
                FloatBuffer after = BufferUtils.createFloatBuffer(16);
                GL11.glGetFloat(GL11.GL_CURRENT_COLOR, after);
                require(
                    mode == GL11.glGetInteger(GL11.GL_MATRIX_MODE)
                        && stackDepth == GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH),
                    "particle matrix state leak");
                require(
                    blend == GL11.glIsEnabled(GL11.GL_BLEND) && depth == GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
                        && texture == GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                    "particle enable state leak");
                for (int i = 0; i < 4; i++) require(color.get(i) == after.get(i), "particle color leak");
                require(
                    shade == GL11.glGetInteger(GL11.GL_SHADE_MODEL)
                        && blendSource == GL11.glGetInteger(GL11.GL_BLEND_SRC)
                        && blendDestination == GL11.glGetInteger(GL11.GL_BLEND_DST),
                    "sparkle blend/shade state leak");
                glCheck("particle state restoration");
                float tilted = probe.angle;
                for (int i = 0; i < 55; i++) {
                    Thread.sleep(16);
                    panel.drawScreen(panel.left() + 110, panel.top() + 75, 0);
                }
                require(Math.abs(probe.angle) < Math.abs(tilted) * 0.15, "carried rotation did not settle");
                require(UiEffects.particleCount() == 0, "stationary trails did not expire");
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(16);
                    panel.drawScreen(panel.left() + 35 + i * 6, panel.top() + 75, 0);
                }
                require(UiEffects.particleCount() > 0, "stall test did not prime trail");
                Thread.sleep(800);
                panel.drawScreen(panel.left() + 130, panel.top() + 75, 0);
                require(UiEffects.particleCount() == 0, "stall retained or emitted old trail");
                glCheck("stall", baselineError);
                UiEffectsConfig.enabled = false;
                panel.drawScreen(panel.left() + 110, panel.top() + 75, 0);
                require(
                    Math.abs(probe.scale - 1) < 0.001 && Math.abs(probe.angle) < 0.001,
                    "disabled carried transform");
                UiEffectsConfig.enabled = true;
                UiEffectsConfig.hover = true;
                mc.thePlayer.inventory.setItemStack(null);
                verifyHotbar(mc, res);
                glCheck("hotbar", baselineError);
                int enabledError = GL11.glGetError();
                System.out.println("LEGACYVISUALFIX_UI_GL enabledCore=" + enabledError);
                require(enabledError == 0 || enabledError == baselineError, "new core GL error " + enabledError);
                MinecraftForgeClient.registerItemRenderer(Items.diamond, null);
                MinecraftForgeClient.registerItemRenderer(Items.nether_star, null);
                renderDemo(mc, dir);
                if (cpw.mods.fml.common.Loader.isModLoaded("modularui")
                    && cpw.mods.fml.common.Loader.isModLoaded("modularui2")) {
                    Class.forName("com.legacyvisualfix.smoke.UiMuiSmoke")
                        .getMethod("verify", Minecraft.class)
                        .invoke(null, mc);
                }
                result = "PASS hover/original renderer/count; matching/count/NBT/metadata; carried tilt/settle; trails ("
                    + oldParticles
                    + ")/expire/stall/GL state; disable; real Forge hotbar; optional MUI checks when loaded";
            } catch (Throwable e) {
                e.printStackTrace();
                result = "FAIL " + e;
            }
            try {
                Files.write(new File(dir, "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("LEGACYVISUALFIX_UI_SMOKE " + result);
            mc.shutdown();
        }
    }

    private static class Panel extends GuiContainer {

        Panel() {
            super(new Container() {

                {
                    InventoryBasic inv = new InventoryBasic("test", true, 1);
                    inv.setInventorySlotContents(0, new ItemStack(Items.diamond, 17));
                    addSlotToContainer(new Slot(inv, 0, 20, 20));
                }

                @Override
                public boolean canInteractWith(EntityPlayer p) {
                    return true;
                }
            });
        }

        int left() {
            return guiLeft;
        }

        int top() {
            return guiTop;
        }

        @Override
        protected void drawGuiContainerBackgroundLayer(float t, int x, int y) {
            drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xff243447);
        }
    }

    private static class Probe implements IItemRenderer {

        float scale, angle, posY, slotY;

        @Override
        public boolean handleRenderType(ItemStack s, ItemRenderType t) {
            return t == ItemRenderType.INVENTORY;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType t, ItemStack s, ItemRendererHelper h) {
            return false;
        }

        @Override
        public void renderItem(ItemRenderType t, ItemStack s, Object... data) {
            FloatBuffer m = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, m);
            scale = (float) Math.sqrt(m.get(0) * m.get(0) + m.get(1) * m.get(1));
            angle = (float) Math.toDegrees(Math.atan2(m.get(1), m.get(0)));
            posY = m.get(13);
            if (s.stackSize == 17) slotY = posY;
            GuiScreen.drawRect(2, 2, 14, 14, 0xff48dbe3);
        }
    }

    private static void verifyHotbar(Minecraft mc, ScaledResolution res) throws Exception {
        Bar bar = new Bar(mc);
        java.lang.reflect.Field event = GuiIngameForge.class.getDeclaredField("eventParent");
        event.setAccessible(true);
        event.set(bar, new RenderGameOverlayEvent(0, res, 0, 0));
        mc.thePlayer.inventory.currentItem = 0;
        bar.draw(res);
        int origin = bar.selector;
        require(bar.selectorCalls == 1 && bar.itemsBeforeSelector == 9, "selector must draw once after all items");
        require(!bar.selectorDepth, "selector must ignore item depth");
        mc.thePlayer.inventory.currentItem = 8;
        Thread.sleep(16);
        bar.draw(res);
        require(bar.selector > origin && bar.selector < origin + 160, "hotbar did not interpolate");
        require(mc.thePlayer.inventory.currentItem == 8, "hotbar selection delayed");
        for (int i = 0; i < 32; i++) {
            Thread.sleep(12);
            bar.draw(res);
        }
        require(Math.abs(bar.selector - origin - 160) <= 1, "hotbar did not converge");
        UiEffectsConfig.hotbar = false;
        mc.thePlayer.inventory.currentItem = 0;
        bar.draw(res);
        require(bar.selector == origin, "disabled hotbar did not snap");
        require(bar.selectorCalls == 1 && bar.itemsBeforeSelector == 0, "disabled hotbar changed vanilla order");
        UiEffectsConfig.hotbar = true;
    }

    private static class Bar extends GuiIngameForge {

        int selector;
        int items, itemsBeforeSelector, selectorCalls;
        boolean selectorDepth;

        Bar(Minecraft mc) {
            super(mc);
        }

        void draw(ScaledResolution res) {
            items = 0;
            selectorCalls = 0;
            renderHotbar(res.getScaledWidth(), res.getScaledHeight(), 0);
        }

        @Override
        protected void renderInventorySlot(int slot, int x, int y, float ticks) {
            items++;
            super.renderInventorySlot(slot, x, y, ticks);
        }

        @Override
        public void drawTexturedModalRect(int x, int y, int u, int v, int w, int h) {
            if (u == 0 && v == 22 && w == 24) {
                selector = x;
                selectorCalls++;
                itemsBeforeSelector = items;
                selectorDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            }
            super.drawTexturedModalRect(x, y, u, v, w, h);
        }
    }

    private static void renderDemo(Minecraft mc, File directory) throws Exception {
        InventoryBasic inv = new InventoryBasic("LegacyVisualFix UI effects", true, 27);
        ItemStack star = new ItemStack(Items.nether_star, 8);
        star.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking, 1);
        for (int i = 0; i < 18; i++) inv.setInventorySlotContents(
            i,
            i % 3 == 0 ? star.copy() : new ItemStack(i % 3 == 1 ? Items.diamond : Items.gold_ingot, i + 1));
        GuiContainer chest = new net.minecraft.client.gui.inventory.GuiChest(mc.thePlayer.inventory, inv);
        mc.currentScreen = chest;
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        chest.setWorldAndResolution(mc, res.getScaledWidth(), res.getScaledHeight());
        mc.thePlayer.inventory.setItemStack(star.copy());
        mc.entityRenderer.setupOverlayRendering();
        int cy = res.getScaledHeight() / 2;
        for (int i = 0; i < 44; i++) {
            Thread.sleep(16);
            GL11.glClearColor(.08f, .11f, .16f, 1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            int path = Math.min(i, 23);
            chest.drawScreen(res.getScaledWidth() / 2 - 60 + path * 4, cy - 20 + (int) (Math.sin(path * .22) * 16), 0);
            if (i % 2 == 0) captureFrame(mc, new File(directory, String.format("sparkle-%02d.png", i / 2)));
            if (i == 23) captureFrame(mc, new File(directory, "five-effects.png"));
        }
        mc.thePlayer.inventory.setItemStack(null);
    }

    private static void captureFrame(Minecraft mc, File file) throws Exception {
        ByteBuffer pixels = BufferUtils.createByteBuffer(mc.displayWidth * mc.displayHeight * 4);
        GL11.glReadPixels(0, 0, mc.displayWidth, mc.displayHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        BufferedImage image = new BufferedImage(mc.displayWidth, mc.displayHeight, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < mc.displayHeight; y++) for (int x = 0; x < mc.displayWidth; x++) {
            int n = ((mc.displayHeight - 1 - y) * mc.displayWidth + x) * 4;
            image
                .setRGB(x, y, (pixels.get(n) & 255) << 16 | (pixels.get(n + 1) & 255) << 8 | (pixels.get(n + 2) & 255));
        }
        ImageIO.write(image, "png", file);
    }

    private static void require(boolean v, String s) {
        if (!v) throw new AssertionError(s);
    }

    private static void glCheck(String stage) {
        int error = GL11.glGetError();
        require(error == 0, "GL error " + error + " at " + stage);
    }

    private static void glCheck(String stage, int control) {
        int error = GL11.glGetError();
        require(
            error == 0 || error == control,
            "new GL error " + error + " at " + stage + " (disabled=" + control + ")");
    }
}
