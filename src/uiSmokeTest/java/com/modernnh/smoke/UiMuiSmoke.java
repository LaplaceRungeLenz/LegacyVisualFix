package com.modernnh.smoke;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.modernnh.ui.UiEffects;
import com.modernnh.ui.UiEffectsConfig;

import cpw.mods.fml.common.Loader;

/** Optional integration: real public GUI builders and transformed item draw paths, no callback reflection. */
public final class UiMuiSmoke {

    private UiMuiSmoke() {}

    public static void verify(Minecraft mc) throws Exception {
        Probe probe = new Probe();
        MinecraftForgeClient.registerItemRenderer(Items.emerald, probe);
        if (Loader.isModLoaded("modularui")) Mui1.verify(mc, probe);
        if (Loader.isModLoaded("modularui2")) Mui2.verify(mc, probe);
        mc.thePlayer.inventory.setItemStack(null);
        UiEffects.clear();
    }

    private interface Draw {

        void render(int x, int y);
    }

    private static void exercise(Minecraft mc, Probe probe, String name, Draw draw,
        java.util.function.IntSupplier slotX, java.util.function.IntSupplier slotY,
        java.util.function.IntSupplier ghostX, java.util.function.IntSupplier ghostY) throws Exception {
        UiEffectsConfig.enabled = false;
        UiEffectsConfig.hover = true;
        UiEffects.clear();
        mc.thePlayer.inventory.setItemStack(null);
        while (GL11.glGetError() != 0) {}
        // Settle the UI library's own opening animation before comparing item transforms.
        for (int i = 0; i < 55; i++) render(draw, -1000, -1000);
        for (int i = 0; i < 24; i++) render(draw, slotX.getAsInt(), slotY.getAsInt());
        int baselineError = GL11.glGetError();
        System.out.println("MODERNNH_UI_GL " + name + " disabled=" + baselineError);
        UiEffectsConfig.enabled = true;
        require(probe.normalCalls > 0 && probe.ghostCalls > 0, name + " did not render both real widgets");
        float baseline = probe.normalScale;
        float ghostBaseline = probe.ghostScale;
        for (int i = 0; i < 24; i++) render(draw, slotX.getAsInt(), slotY.getAsInt());
        require(
            probe.normalScale > baseline * 1.08f,
            name + " hover missing: " + baseline + " -> " + probe.normalScale);
        for (int i = 0; i < 24; i++) render(draw, ghostX.getAsInt(), ghostY.getAsInt());
        require(Math.abs(probe.ghostScale - ghostBaseline) < 0.01f, name + " phantom animated");
        ItemStack carried = new ItemStack(Items.emerald, 3);
        carried.addEnchantment(net.minecraft.enchantment.Enchantment.unbreaking, 1);
        mc.thePlayer.inventory.setItemStack(carried);
        for (int i = 0; i < 18; i++) render(draw, slotX.getAsInt() + 40 + i * 4, slotY.getAsInt() + 50);
        require(probe.carriedCalls > 0 && probe.carriedScale > baseline * 1.08f, name + " carried scaling missing");
        require(Math.abs(probe.carriedAngle) > 0.5f, name + " carried tilt missing");
        require(UiEffects.particleCount() > 0, name + " rare carried trail missing");
        mc.thePlayer.inventory.setItemStack(null);
        int enabledError = GL11.glGetError();
        System.out.println("MODERNNH_UI_GL " + name + " enabled=" + enabledError);
        require(enabledError == 0 || enabledError == baselineError, name + " new GL error " + enabledError);
        System.out
            .println("MODERNNH_UI_MUI_SMOKE PASS " + name + " real widgets/hover/phantom/carried/trail/matrix depth");
    }

    private static void render(Draw draw, int x, int y) throws Exception {
        Thread.sleep(16);
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        draw.render(x, y);
        require(GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) == depth, "MUI modelview stack leak");
    }

    private static final class Mui1 {

        static void verify(Minecraft mc, Probe probe) throws Exception {
            com.gtnewhorizons.modularui.api.forge.ItemStackHandler items = new com.gtnewhorizons.modularui.api.forge.ItemStackHandler(
                2);
            items.setStackInSlot(0, new ItemStack(Items.emerald, 17));
            items.setStackInSlot(1, new ItemStack(Items.emerald, 19));
            com.gtnewhorizons.modularui.common.widget.SlotWidget slot = new com.gtnewhorizons.modularui.common.widget.SlotWidget(
                new com.gtnewhorizons.modularui.common.internal.wrapper.BaseSlot(items, 0));
            com.gtnewhorizons.modularui.common.widget.SlotWidget ghost = new com.gtnewhorizons.modularui.common.widget.SlotWidget(
                com.gtnewhorizons.modularui.common.internal.wrapper.BaseSlot.phantom(items, 1));
            slot.setPos(20, 20);
            ghost.setPos(55, 20);
            com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui gui = com.gtnewhorizons.modularui.ModularUI
                .createGuiScreen(mc.thePlayer, context -> {
                    context.setShowNEI(false);
                    return com.gtnewhorizons.modularui.api.screen.ModularWindow.builder(176, 100)
                        .widget(slot)
                        .widget(ghost)
                        .build();
                });
            mc.displayGuiScreen(gui);
            exercise(mc, probe, "MUI1", (x, y) -> {
                // MUI1 updates its topmost cursor target in updateScreen, using the preceding draw's mouse position.
                // The synchronous smoke loop must drive the public tick callback as Minecraft normally does.
                gui.updateScreen();
                gui.drawScreen(x, y, 0);
            },
                () -> slot.getAbsolutePos().x + 9,
                () -> slot.getAbsolutePos().y + 9,
                () -> ghost.getAbsolutePos().x + 9,
                () -> ghost.getAbsolutePos().y + 9);
            require(
                items.getStackInSlot(0).stackSize == 17 && items.getStackInSlot(1).stackSize == 19,
                "MUI1 inventory mutated");
        }
    }

    private static final class Mui2 {

        static void verify(Minecraft mc, Probe probe) throws Exception {
            com.cleanroommc.modularui.utils.item.ItemStackHandler items = new com.cleanroommc.modularui.utils.item.ItemStackHandler(
                2);
            items.setStackInSlot(0, new ItemStack(Items.emerald, 17));
            items.setStackInSlot(1, new ItemStack(Items.emerald, 19));
            com.cleanroommc.modularui.widgets.slot.ItemSlot slot = new com.cleanroommc.modularui.widgets.slot.ItemSlot()
                .slot(items, 0)
                .left(20)
                .top(20);
            com.cleanroommc.modularui.widgets.slot.ItemSlot ghost = new com.cleanroommc.modularui.widgets.slot.PhantomItemSlot()
                .slot(new com.cleanroommc.modularui.widgets.slot.ModularSlot(items, 1))
                .left(55)
                .top(20);
            com.cleanroommc.modularui.screen.ModularPanel panel = com.cleanroommc.modularui.screen.ModularPanel
                .defaultPanel("modernnh_smoke", 176, 100)
                .child(slot)
                .child(ghost);
            com.cleanroommc.modularui.value.sync.ModularSyncManager sync = new com.cleanroommc.modularui.value.sync.ModularSyncManager(
                true);
            com.cleanroommc.modularui.value.sync.PanelSyncManager psm = new com.cleanroommc.modularui.value.sync.PanelSyncManager(
                sync,
                true);
            com.cleanroommc.modularui.widget.WidgetTree.collectSyncValues(psm, panel);
            com.cleanroommc.modularui.screen.UISettings settings = new com.cleanroommc.modularui.screen.UISettings();
            com.cleanroommc.modularui.screen.ModularScreen screen = new com.cleanroommc.modularui.screen.ModularScreen(
                "modernnh",
                panel);
            screen.getContext()
                .setSettings(settings);
            com.cleanroommc.modularui.screen.ModularContainer container = new com.cleanroommc.modularui.screen.ModularContainer();
            container.construct(
                mc.thePlayer,
                sync,
                settings,
                panel.getName(),
                new com.cleanroommc.modularui.factory.GuiData(mc.thePlayer));
            GuiContainer gui = new com.cleanroommc.modularui.screen.GuiContainerWrapper(container, screen);
            mc.displayGuiScreen(gui);
            probe.reset();
            exercise(mc, probe, "MUI2", (x, y) -> {
                screen.getContext()
                    .updateState(x, y, 0);
                screen.onFrameUpdate();
                com.cleanroommc.modularui.screen.ClientScreenHandler.drawScreen(screen, gui, x, y, 0);
            },
                () -> slot.getArea().x + 9,
                () -> slot.getArea().y + 9,
                () -> ghost.getArea().x + 9,
                () -> ghost.getArea().y + 9);
            require(
                items.getStackInSlot(0).stackSize == 17 && items.getStackInSlot(1).stackSize == 19,
                "MUI2 inventory mutated");
        }
    }

    private static final class Probe implements IItemRenderer {

        float normalScale, ghostScale, carriedScale, carriedAngle;
        int normalCalls, ghostCalls, carriedCalls;

        void reset() {
            normalCalls = ghostCalls = carriedCalls = 0;
        }

        @Override
        public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
            return type == ItemRenderType.INVENTORY;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
            return false;
        }

        @Override
        public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
            FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
            float scale = (float) Math.hypot(matrix.get(0), matrix.get(1));
            if (stack.stackSize == 17) {
                normalScale = scale;
                normalCalls++;
            }
            if (stack.stackSize == 19) {
                ghostScale = scale;
                ghostCalls++;
            }
            if (stack.stackSize == 3) {
                carriedScale = scale;
                carriedAngle = (float) Math.toDegrees(Math.atan2(matrix.get(1), matrix.get(0)));
                carriedCalls++;
            }
            GuiScreen.drawRect(2, 2, 14, 14, 0xff3fcf84);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
