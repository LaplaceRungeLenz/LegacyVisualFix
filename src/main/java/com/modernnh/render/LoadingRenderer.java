package com.modernnh.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import com.modernnh.reload.ReloadProgress;
import com.modernnh.theme.Theme;
import com.modernnh.theme.ThemeTextures;

public final class LoadingRenderer implements AutoCloseable {

    private OwnedTexture textTexture;
    private String lastText = "";

    public void draw(Minecraft mc, Theme theme, ThemeTextures textures, ReloadProgress progress, String detail) {
        int width = Math.max(1, Display.getWidth());
        int height = Math.max(1, Display.getHeight());
        ScaledResolution resolution = new ScaledResolution(mc, width, height);
        int w = resolution.getScaledWidth();
        int h = resolution.getScaledHeight();
        try (GLState state = new GLState(width, height, w, h)) {
            GL11.glClearColor(0, 0, 0, 1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            quad(null, 0, 0, w, h, 1, theme.backgroundColor);
            if (textures != null && textures.background != null) {
                OwnedTexture bg = textures.background;
                double bw = w;
                double bh = h;
                if (theme.fit != Theme.Fit.STRETCH) {
                    double sx = (double) w / bg.width;
                    double sy = (double) h / bg.height;
                    double scale = theme.fit == Theme.Fit.COVER ? Math.max(sx, sy) : Math.min(sx, sy);
                    bw = bg.width * scale;
                    bh = bg.height * scale;
                }
                quad(bg, (w - bw) / 2, (h - bh) / 2, bw, bh, 1, 0xFFFFFFFF);
            }
            BarLayout bar = new BarLayout(w, h, theme.barWidth, theme.barHeight, theme.barX, theme.barY);
            quad(textures == null ? null : textures.track, bar.x, bar.y, bar.width, bar.height, 1, theme.trackColor);
            double fraction = progress.getFraction();
            if (fraction > 0) {
                quad(
                    textures == null ? null : textures.fill,
                    bar.x,
                    bar.y,
                    bar.filledWidth(fraction),
                    bar.height,
                    fraction,
                    theme.fillColor);
            }
            if (theme.showText) {
                // Rasterize independently: using Minecraft FontRenderer during its reload can mutate TextureManager.
                String text = "RELOADING RESOURCES  " + progress
                    .getCompleted() + " / " + progress.getTotal() + " stages\n" + progress.getStage() + "\n" + detail;
                updateText(text);
                double tw = Math.min(w - 8, textTexture.width / 2.0);
                double th = textTexture.height * tw / textTexture.width;
                double ty = Math.max(0, Math.min(h - th, bar.y - th - 8));
                quad(textTexture, (w - tw) / 2, ty, tw, th, 1, theme.textColor);
            }
            Display.update(false);
        }
        // Pump window events without dispatching game input or recursively ticking/rendering Minecraft.
        Display.processMessages();
    }

    private void updateText(String text) {
        if (text.equals(lastText) && textTexture != null) return;
        BufferedImage image = new BufferedImage(1024, 112, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
            g.setColor(Color.WHITE);
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < Math.min(3, lines.length); i++) {
                String line = lines[i];
                while (g.getFontMetrics()
                    .stringWidth(line) > 1000 && line.length() > 1) {
                    line = line.substring(0, line.length() - 1);
                }
                g.drawString(
                    line,
                    (1024 - g.getFontMetrics()
                        .stringWidth(line)) / 2,
                    28 + i * 34);
            }
        } finally {
            g.dispose();
        }
        OwnedTexture replacement = new OwnedTexture(image);
        if (textTexture != null) textTexture.close();
        textTexture = replacement;
        lastText = text;
    }

    private static void quad(OwnedTexture texture, double x, double y, double width, double height, double uMax,
        int argb) {
        if (texture == null) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        } else {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.id);
        }
        GL11.glColor4f(
            ((argb >> 16) & 255) / 255f,
            ((argb >> 8) & 255) / 255f,
            (argb & 255) / 255f,
            ((argb >>> 24) & 255) / 255f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d(0, 0);
        GL11.glVertex2d(x, y);
        GL11.glTexCoord2d(0, 1);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2d(uMax, 1);
        GL11.glVertex2d(x + width, y + height);
        GL11.glTexCoord2d(uMax, 0);
        GL11.glVertex2d(x + width, y);
        GL11.glEnd();
    }

    @Override
    public void close() {
        if (textTexture != null) {
            textTexture.close();
            textTexture = null;
        }
    }
}
