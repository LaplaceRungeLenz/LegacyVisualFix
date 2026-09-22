package com.legacyvisualfix.mixin.waila.wdmla;

import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.gtnewhorizons.wdmla.overlay.GuiBlockDraw;
import com.legacyvisualfix.waila.TooltipContentTransform;
import com.legacyvisualfix.waila.WdmlaAnimationRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** WDMla's world model uses a separate viewport and ignores the foreground model-view matrix. */
@Mixin(value = GuiBlockDraw.class, remap = false)
public abstract class MixinGuiBlockDraw {

    @Unique
    private static final IntBuffer legacyvisualfix$scissorBox = BufferUtils.createIntBuffer(16);

    @WrapMethod(method = "render")
    private void legacyvisualfix$viewport(int x, int y, int width, int height, Operation<Void> original) {
        TooltipContentTransform content = WdmlaAnimationRenderer.contentTransform();
        if (content != null && content.scale != 1) {
            x = content.viewportX(x, WdmlaAnimationRenderer.pixelScale());
            y = content.viewportY(y, WdmlaAnimationRenderer.pixelScale(), Minecraft.getMinecraft().displayHeight);
            width = content.viewportSize(width);
            height = content.viewportSize(height);
            if (width == 0 || height == 0) return;
        }
        if (content == null) {
            original.call(x, y, width, height);
            return;
        }
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        legacyvisualfix$scissorBox.clear();
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, legacyvisualfix$scissorBox);
        int oldX = legacyvisualfix$scissorBox.get(0), oldY = legacyvisualfix$scissorBox.get(1);
        int oldWidth = legacyvisualfix$scissorBox.get(2), oldHeight = legacyvisualfix$scissorBox.get(3);
        try {
            original.call(x, y, width, height);
        } finally {
            // Angelica's emulated attribute stack does not restore the scissor rectangle.
            GL11.glScissor(oldX, oldY, oldWidth, oldHeight);
            if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @WrapMethod(method = "scissorView")
    private void legacyvisualfix$scissor(int x, int y, int width, int height, Operation<Void> original) {
        if (WdmlaAnimationRenderer.contentTransform() == null) {
            original.call(x, y, width, height);
            return;
        }
        int right = x + width, top = y + height;
        if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
            IntBuffer box = legacyvisualfix$scissorBox;
            box.clear();
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, box);
            x = Math.max(x, box.get(0));
            y = Math.max(y, box.get(1));
            right = Math.min(right, box.get(0) + box.get(2));
            top = Math.min(top, box.get(1) + box.get(3));
        }
        // The outer render wrapper restores this rectangle even with an emulated attribute stack.
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, Math.max(0, right - x), Math.max(0, top - y));
    }
}
