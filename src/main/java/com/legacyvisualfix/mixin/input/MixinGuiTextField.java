package com.legacyvisualfix.mixin.input;

import java.text.BreakIterator;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.input.TextSelectionScreen;

@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField {

    @Shadow
    @Final
    private FontRenderer field_146211_a;

    @Shadow
    private int lineScrollOffset;

    @Unique
    private long legacyvisualfix$lastClick;

    @Unique
    private int legacyvisualfix$lastClickX;

    @Unique
    private int legacyvisualfix$lastClickY;

    @Unique
    private int legacyvisualfix$dragStartX;

    @Unique
    private boolean legacyvisualfix$dragging;

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void legacyvisualfix$beginSelection(int x, int y, int button, CallbackInfo ci) {
        GuiTextField field = (GuiTextField) (Object) this;
        if (button != 0) return;
        if (!field.isFocused() || !field.getVisible()
            || x < field.xPosition
            || x >= field.xPosition + field.width
            || y < field.yPosition
            || y >= field.yPosition + field.height) {
            legacyvisualfix$lastClick = 0L;
            return;
        }

        long now = System.nanoTime();
        if (legacyvisualfix$lastClick != 0L && now - legacyvisualfix$lastClick <= 400_000_000L
            && Math.abs(x - legacyvisualfix$lastClickX) <= 4
            && Math.abs(y - legacyvisualfix$lastClickY) <= 4) {
            legacyvisualfix$selectWord(field);
            legacyvisualfix$lastClick = 0L;
        } else {
            legacyvisualfix$lastClick = now;
            legacyvisualfix$lastClickX = x;
            legacyvisualfix$lastClickY = y;
        }

        legacyvisualfix$dragStartX = x;
        legacyvisualfix$dragging = false;
        if (Minecraft.getMinecraft().currentScreen instanceof TextSelectionScreen screen) {
            screen.legacyvisualfix$startTextSelection(this::legacyvisualfix$extendSelection);
        }
    }

    @Unique
    private void legacyvisualfix$selectWord(GuiTextField field) {
        String text = field.getText();
        int cursor = field.getCursorPosition();
        if (cursor >= text.length()) return;

        BreakIterator words = BreakIterator.getWordInstance(Locale.ROOT);
        words.setText(text);
        int end = words.following(cursor);
        int start = words.previous();
        if (start != BreakIterator.DONE && end != BreakIterator.DONE) {
            field.setCursorPosition(start);
            field.setSelectionPos(end);
        }
    }

    @Unique
    private void legacyvisualfix$extendSelection(int x) {
        GuiTextField field = (GuiTextField) (Object) this;
        if (!field.isFocused() || !field.getVisible()) return;
        if (!legacyvisualfix$dragging && Math.abs(x - legacyvisualfix$dragStartX) <= 2) return;

        legacyvisualfix$dragging = true;
        legacyvisualfix$lastClick = 0L;
        int width = field.getWidth();
        int pixel = x - field.xPosition - (field.getEnableBackgroundDrawing() ? 4 : 0);
        String visible = field_146211_a.trimStringToWidth(
            field.getText()
                .substring(lineScrollOffset),
            width);
        int selection = lineScrollOffset + field_146211_a.trimStringToWidth(visible, Math.max(0, pixel))
            .length();
        if (pixel < 0) selection--;
        if (pixel > width) selection++;
        field.setSelectionPos(selection);
    }
}
