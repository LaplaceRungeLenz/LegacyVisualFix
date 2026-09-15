package com.modernnh.smoke;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.modernnh.inventory.InventoryAnimationConfig;
import com.modernnh.inventory.InventoryAnimations;
import com.modernnh.inventory.InventoryMotion;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Real transformed GUI/OpenGL checks, opt-in and excluded from deliverable jars. */
@Mod(
    modid = "modernnhinventorysmoke",
    name = "ModernNH inventory smoke",
    version = "1",
    dependencies = "required-after:modernnh")
public final class InventorySmoke {

    private int ticks;
    private static int mousePre;
    private static int mousePost;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        // Exercise Forge's extra creative pages and their buttons/counter.
        for (int i = 0; i < 3; i++) {
            new CreativeTabs("modernnhsmoke" + i) {

                @Override
                public Item getTabIconItem() {
                    return Items.apple;
                }
            };
        }
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void inputEvent(cpw.mods.fml.common.eventhandler.Event event) {
        String name = event.getClass()
            .getName();
        if (name.equals("com.cleanroommc.modularui.api.event.MouseInputEvent$Pre")) mousePre++;
        if (name.equals("com.cleanroommc.modularui.api.event.MouseInputEvent$Post")) mousePost++;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ++ticks == 40) Minecraft.getMinecraft()
            .displayGuiScreen(new TestScreen());
    }

    private static final class TestScreen extends GuiScreen {

        private boolean done;
        private final File directory = new File(Minecraft.getMinecraft().mcDataDir, "modernnh-inventory-smoke");
        private int buttonDraws;
        private float buttonY;
        private int neiDraws;
        private float neiY;
        private int baselineError;

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            if (done) return;
            done = true;
            int oldScale = mc.gameSettings.guiScale;
            try {
                directory.mkdirs();
                NetHandlerPlayClient handler = new NetHandlerPlayClient(mc, null, null) {

                    @Override
                    public void addToSendQueue(net.minecraft.network.Packet packet) {}
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
                mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 2400));
                mc.thePlayer.inventory.setInventorySlotContents(0, new ItemStack(Items.apple, 12));
                mc.thePlayer.inventory.setInventorySlotContents(9, new ItemStack(Items.diamond, 32));
                RenderManager.instance.cacheActiveRenderInfo(
                    world,
                    mc.getTextureManager(),
                    mc.fontRenderer,
                    mc.thePlayer,
                    mc.thePlayer,
                    mc.gameSettings,
                    0);
                if (Loader.isModLoaded("NotEnoughItems")) {
                    Class<?> config = Class.forName("codechicken.nei.NEIClientConfig");
                    config.getMethod("loadWorld", String.class)
                        .invoke(null, "modernnh-inventory-smoke");
                    config.getMethod("setEnabled", boolean.class)
                        .invoke(null, true);
                    Class<?> drawHandler = Class.forName("codechicken.nei.guihook.IContainerDrawHandler");
                    Object observer = java.lang.reflect.Proxy.newProxyInstance(
                        drawHandler.getClassLoader(),
                        new Class<?>[] { drawHandler },
                        (proxy, method, args) -> {
                            if (method.getName()
                                .equals("renderObjects")) {
                                neiDraws++;
                                neiY = matrixY();
                            }
                            if (method.getName()
                                .equals("equals")) return proxy == args[0];
                            if (method.getName()
                                .equals("hashCode")) return System.identityHashCode(proxy);
                            if (method.getName()
                                .equals("toString")) return "ModernNH smoke observer";
                            return null;
                        });
                    Class.forName("codechicken.nei.guihook.GuiContainerManager")
                        .getMethod("addDrawHandler", drawHandler)
                        .invoke(null, observer);
                }
                InventoryAnimationConfig.enabled = true;
                InventoryAnimationConfig.durationMs = 250;
                InventoryAnimationConfig.distance = 32;
                // Control: distinguish renderer warm-up/state differences from our translation.
                if (Loader.isModLoaded("angelica")) {
                    InventoryAnimationConfig.enabled = false;
                    mc.gameSettings.guiScale = 1;
                    mc.entityRenderer.setupOverlayRendering();
                    GuiInventory baseline = new GuiInventory(mc.thePlayer);
                    initialize(baseline);
                    require(GL11.glGetError() == 0, "pre-existing control error");
                    render(baseline);
                    BufferedImage first = capture("control-disabled-first.png");
                    baselineError = GL11.glGetError();
                    System.out.println("INVENTORY_CONTROL first=" + baselineError);
                    render(baseline);
                    BufferedImage second = capture("control-disabled-second.png");
                    int secondError = GL11.glGetError();
                    System.out.println("INVENTORY_CONTROL second=" + secondError);
                    require(secondError == baselineError, "inconsistent disabled control errors");
                    int px = coordinate(baseline, "guiLeft") - 115, py = coordinate(baseline, "guiTop") + 8;
                    System.out
                        .println("INVENTORY_CONTROL potion=" + first.getRGB(px, py) + "," + second.getRGB(px, py));
                    baseline.onGuiClosed();
                    InventoryAnimationConfig.enabled = true;
                }
                for (int scale : new int[] { 1, 2 }) {
                    mc.gameSettings.guiScale = scale;
                    mc.entityRenderer.setupOverlayRendering();
                    mc.playerController.setGameType(WorldSettings.GameType.SURVIVAL);
                    verify(new GuiInventory(mc.thePlayer), "survival-" + scale);
                    mc.playerController.setGameType(WorldSettings.GameType.CREATIVE);
                    verify(new GuiContainerCreative(mc.thePlayer), "creative-" + scale);
                    verify(new GuiContainerCreative(mc.thePlayer), "creative-search-" + scale);
                    verify(new GuiContainerCreative(mc.thePlayer), "creative-inventory-" + scale);
                }
                mc.playerController.setGameType(WorldSettings.GameType.SURVIVAL);
                verifyEarlyClick();
                verifyScreenReturns();
                require(
                    InventoryAnimations
                        .motion(new GuiChest(mc.thePlayer.inventory, new InventoryBasic("test", false, 27))) == null,
                    "chest must be excluded");
                require(
                    InventoryAnimations.motion(new GuiInventory(mc.thePlayer) {}) == null,
                    "modded subclass must be excluded");
                // Verify the scope restores the model-view stack even if a renderer throws.
                GuiInventory scope = new GuiInventory(mc.thePlayer);
                initialize(scope);
                InventoryAnimations.motion(scope)
                    .frame(0, 400, 100);
                float before = matrixY();
                try {
                    InventoryAnimations.draw(scope, () -> { throw new IllegalStateException("expected"); });
                } catch (IllegalStateException expected) {
                    require("expected".equals(expected.getMessage()), "wrong exception");
                }
                require(Math.abs(matrixY() - before) < 0.001, "exception leaked matrix");
                require(GL11.glGetError() == 0, "animation-only scope added GL error");
                finish(
                    "PASS: survival/creative at scales 1/2; real left/right pickup/place; ModularUI input events; consumed press/release; world/creative redirect and companion returns; translated buttons/pixels; stationary potion/NEI; natural expiry; resize; unrelated containers excluded; exception scope restored; NEI="
                        + Loader.isModLoaded("NotEnoughItems")
                        + "; disabled-control GL="
                        + baselineError);
            } catch (Throwable failure) {
                failure.printStackTrace();
                finish("FAIL: " + failure);
            } finally {
                mc.gameSettings.guiScale = oldScale;
                mc.currentScreen = this;
                mc.theWorld = null;
                mc.thePlayer = null;
                mc.renderViewEntity = null;
                mc.shutdown();
            }
        }

        private void initialize(GuiScreen screen) {
            ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            mc.currentScreen = screen;
            screen.setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
        }

        private void verifyScreenReturns() throws Exception {
            mc.currentScreen = null;
            mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            require(InventoryAnimations.entering(mc.currentScreen), "world opening did not animate");
            mc.displayGuiScreen(new GuiScreen());
            int before = mousePre;
            click(mc.currentScreen, 4, 4, 0);
            if (Loader.isModLoaded("modularui2")) require(mousePre > before, "unrelated GUI lost input events");
            mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            require(!InventoryAnimations.entering(mc.currentScreen), "return from another GUI replayed animation");
            mc.currentScreen = null;
            mc.playerController.setGameType(WorldSettings.GameType.CREATIVE);
            mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            require(
                mc.currentScreen instanceof GuiContainerCreative && InventoryAnimations.entering(mc.currentScreen),
                "initial creative redirect lost animation");
            mc.displayGuiScreen(new GuiScreen());
            mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            require(!InventoryAnimations.entering(mc.currentScreen), "creative return replayed animation");
            mc.playerController.setGameType(WorldSettings.GameType.SURVIVAL);
            if (Loader.isModLoaded("Baubles")) {
                GuiScreen baubles = (GuiScreen) Class.forName("baubles.client.gui.GuiPlayerExpanded")
                    .getConstructor(net.minecraft.entity.player.EntityPlayer.class)
                    .newInstance(mc.thePlayer);
                verifyReturn(baubles, "Baubles");
            }
            if (Loader.isModLoaded("cosmeticarmorreworked")) {
                Object manager = Class.forName("lain.mods.cos.CosmeticArmorReworked")
                    .getField("invMan")
                    .get(null);
                Object armor = manager.getClass()
                    .getMethod("getCosArmorInventoryClient", java.util.UUID.class)
                    .invoke(manager, mc.thePlayer.getUniqueID());
                Object container = Class.forName("lain.mods.cos.inventory.ContainerCosArmor")
                    .getConstructors()[0].newInstance(mc.thePlayer.inventory, armor, mc.thePlayer);
                GuiScreen cosmetic = (GuiScreen) Class.forName("lain.mods.cos.client.GuiCosArmorInventory")
                    .getConstructor(net.minecraft.inventory.Container.class)
                    .newInstance(container);
                verifyReturn(cosmetic, "CosmeticArmor");
            }
            System.out.println("INVENTORY_LIFECYCLE world/return/creative redirect PASS");
        }

        private void verifyReturn(GuiScreen companion, String name) {
            mc.displayGuiScreen(companion);
            require(InventoryAnimations.motion(companion) == null, name + " was animated");
            mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
            require(!InventoryAnimations.entering(mc.currentScreen), name + " return replayed animation");
            System.out.println("INVENTORY_RETURN " + name + " PASS");
        }

        @SuppressWarnings("unchecked")
        private void verify(GuiContainer gui, String name) throws Exception {
            initialize(gui);
            if (gui instanceof GuiContainerCreative) {
                Method select = GuiContainerCreative.class
                    .getDeclaredMethod("setCurrentCreativeTab", CreativeTabs.class);
                select.setAccessible(true);
                select.invoke(
                    gui,
                    name.contains("search") ? CreativeTabs.tabAllSearch
                        : name.contains("inventory") ? CreativeTabs.tabInventory : CreativeTabs.tabBlock);
                require(InventoryAnimations.entering(gui), "tab selection interrupted entrance");
            }
            int initialTop = coordinate(gui, "guiTop");
            GuiButton button = new GuiButton(999, 10, 10, "smoke") {

                @Override
                public void drawButton(Minecraft mc, int x, int y) {
                    buttonDraws++;
                    buttonY = matrixY();
                }
            };
            Field buttons = GuiScreen.class.getDeclaredField("buttonList");
            buttons.setAccessible(true);
            ((java.util.List<GuiButton>) buttons.get(gui)).add(button);
            int drawsBefore = buttonDraws;
            int neiBefore = neiDraws;
            float baseY = matrixY();
            render(gui);
            InventoryMotion motion = InventoryAnimations.motion(gui);
            require(motion != null && motion.isEntering(), "animation did not start");
            require(Math.abs(motion.offset() - 32) < 0.001, "first frame did not start at 32");
            require(buttonDraws > drawsBefore && Math.abs(buttonY - baseY - 32) < 0.01, "button not translated");
            require(Math.abs(matrixY() - baseY) < 0.01, "draw leaked matrix");
            if (Loader.isModLoaded("NotEnoughItems"))
                require(neiDraws > neiBefore && Math.abs(neiY - baseY) < 0.01, "NEI not stationary");
            int top = coordinate(gui, "guiTop");
            int left = coordinate(gui, "guiLeft");
            require(top == initialTop, "layout was moved");
            Method region = GuiContainer.class
                .getDeclaredMethod("func_146978_c", int.class, int.class, int.class, int.class, int.class, int.class);
            region.setAccessible(true);
            require(!(Boolean) region.invoke(gui, 0, 0, 16, 16, left + 8, top + 8), "stale hover active");
            BufferedImage moving = capture(name + "-moving.png");
            Thread.sleep(InventoryAnimationConfig.durationMs + 30L);
            render(gui);
            require(!motion.isEntering(), "animation did not expire naturally");
            require(Math.abs(buttonY - baseY) < 0.01, "button not settled");
            require((Boolean) region.invoke(gui, 0, 0, 16, 16, left + 8, top + 8), "hover not restored");
            BufferedImage settled = capture(name + "-settled.png");
            if (name.startsWith("survival") || name.startsWith("creative-inventory")) verifyClicks(gui);
            int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
            // A background pixel moves exactly 32 GUI pixels. The potion icon stays put.
            int sx = (left + 4) * scale, sy = (top + 4) * scale;
            require(moving.getRGB(sx, sy + 32 * scale) == settled.getRGB(sx, sy), "panel pixels not translated");
            int potionX = (left - 115) * scale, potionY = (top + 8) * scale;
            require(potionX >= 0, "potion sample outside screen");
            require(moving.getRGB(potionX, potionY) == settled.getRGB(potionX, potionY), "potion moved");
            gui.initGui();
            require(!motion.isEntering(), "resize replayed animation");
            gui.onGuiClosed();
            int renderError = GL11.glGetError();
            System.out.println("INVENTORY_SCENARIO " + name + " GL=" + renderError);
            require(
                renderError == 0 || renderError == baselineError,
                "new GL error " + renderError + " (disabled=" + baselineError + ")");
        }

        private void render(GuiScreen gui) {
            GL11.glClearColor(0.13F, 0.16F, 0.2F, 1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            mc.currentScreen = gui;
            gui.drawScreen(-1000, -1000, 0);
        }

        private void verifyClicks(GuiContainer gui) throws Exception {
            int pre = mousePre, post = mousePost;
            mc.thePlayer.openContainer = gui.inventorySlots;
            mc.thePlayer.inventory.setItemStack(null);
            mc.thePlayer.inventory.setInventorySlotContents(9, new ItemStack(Items.diamond, 32));
            net.minecraft.inventory.Slot slot = gui.inventorySlots.getSlot(9);
            int x = coordinate(gui, "guiLeft") + slot.xDisplayPosition + 8;
            int y = coordinate(gui, "guiTop") + slot.yDisplayPosition + 8;
            click(gui, x, y, 0);
            require(mc.thePlayer.inventory.getItemStack() != null, "left click did not pick up stack");
            require(mc.thePlayer.inventory.getItemStack().stackSize == 32, "left click picked wrong count");
            click(gui, x, y, 1);
            require(slot.getHasStack() && slot.getStack().stackSize == 1, "right click did not place one");
            require(mc.thePlayer.inventory.getItemStack().stackSize == 31, "right click cursor count wrong");
            click(gui, x, y, 0);
            require(
                mc.thePlayer.inventory.getItemStack() == null && slot.getStack().stackSize == 32,
                "left click did not return stack");
            click(gui, x, y, 1);
            require(
                mc.thePlayer.inventory.getItemStack().stackSize == 16 && slot.getStack().stackSize == 16,
                "right click did not split stack");
            click(gui, x, y, 0);
            require(
                mc.thePlayer.inventory.getItemStack() == null && slot.getStack().stackSize == 32,
                "split stack did not merge back");
            if (Loader.isModLoaded("modularui2")) {
                require(mousePre > pre && mousePost > post, "ModularUI input events were bypassed");
            }
            System.out.println("INVENTORY_INPUT left/right pickup/place PASS");
        }

        private void verifyEarlyClick() throws Exception {
            for (int button : new int[] { 0, 1 }) {
                GuiInventory gui = new GuiInventory(mc.thePlayer);
                initialize(gui);
                render(gui);
                InventoryMotion motion = InventoryAnimations.motion(gui);
                require(motion.isEntering(), "early-click test did not start animation");
                net.minecraft.inventory.Slot slot = gui.inventorySlots.getSlot(9);
                click(
                    gui,
                    coordinate(gui, "guiLeft") + slot.xDisplayPosition + 8,
                    coordinate(gui, "guiTop") + slot.yDisplayPosition + 8,
                    button);
                require(!motion.isEntering() && !motion.blocksHeldMouse(), "consumed click left input latched");
                require(mc.thePlayer.inventory.getItemStack() == null, "early click moved items");
                verifyClicks(gui);
                gui.onGuiClosed();
            }
            System.out.println("INVENTORY_INPUT early press/release and subsequent click PASS");
        }

        private void click(GuiScreen gui, int x, int y, int button) throws Exception {
            // Feed the real LWJGL event queue, including GuiScreen.handleInput virtual dispatch.
            int nativeX = x * mc.displayWidth / gui.width;
            int nativeY = (gui.height - y - 1) * mc.displayHeight / gui.height;
            if (Loader.isModLoaded("lwjgl3ify")) {
                Class<?> mouse = Class.forName("org.lwjglx.input.Mouse");
                for (String axis : new String[] { "latestX", "latestY" }) {
                    Field coordinate = mouse.getDeclaredField(axis);
                    coordinate.setAccessible(true);
                    coordinate.setInt(null, axis.endsWith("X") ? nativeX : nativeY);
                }
                Method add = mouse.getMethod("addButtonEvent", int.class, boolean.class);
                add.invoke(null, button, true);
                add.invoke(null, button, false);
                gui.handleInput();
                return;
            }
            Field field = org.lwjgl.input.Mouse.class.getDeclaredField("readBuffer");
            field.setAccessible(true);
            ByteBuffer original = (ByteBuffer) field.get(null);
            ByteBuffer events = BufferUtils.createByteBuffer(44);
            for (int down : new int[] { 1, 0 }) {
                events.put((byte) button)
                    .put((byte) down)
                    .putInt(nativeX)
                    .putInt(nativeY)
                    .putInt(0)
                    .putLong(System.nanoTime());
            }
            events.flip();
            try {
                field.set(null, events);
                gui.handleInput();
            } finally {
                field.set(null, original);
            }
        }

        private BufferedImage capture(String name) throws Exception {
            int w = mc.displayWidth, h = mc.displayHeight;
            ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
            GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int i = ((h - 1 - y) * w + x) * 4;
                image.setRGB(
                    x,
                    y,
                    (pixels.get(i) & 255) << 16 | (pixels.get(i + 1) & 255) << 8 | (pixels.get(i + 2) & 255));
            }
            ImageIO.write(image, "png", new File(directory, name));
            return image;
        }

        private void finish(String result) {
            System.out.println("MODERNNH_INVENTORY_SMOKE " + result);
            try {
                Files.write(new File(directory, "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
            } catch (Exception failure) {
                failure.printStackTrace();
            }
        }
    }

    private static int coordinate(GuiContainer gui, String name) throws Exception {
        Field field = GuiContainer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(gui);
    }

    private static float matrixY() {
        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
        return matrix.get(13);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
