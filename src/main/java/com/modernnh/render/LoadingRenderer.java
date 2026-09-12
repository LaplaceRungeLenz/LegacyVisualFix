package com.modernnh.render;

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
    private OwnedTexture titleTexture;
    private String lastTitle = "";

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
            if (theme.showLogo && textures != null && textures.logo != null) {
                OwnedTexture logo = textures.logo;
                double lh = h * theme.logoHeight;
                double lw = lh * logo.width / logo.height;
                double scale = Math.min(1, (w - 8.0) / lw);
                lw *= scale;
                lh *= scale;
                double lx = Math.max(0, Math.min(w - lw, w * theme.logoX - lw / 2));
                double ly = Math.max(0, Math.min(h - lh, h * theme.logoY - lh / 2));
                quad(logo, lx, ly, lw, lh, 1, 0xFFFFFFFF);
            }
            if (!theme.title.isEmpty()) {
                if (titleTexture == null || !lastTitle.equals(theme.title)) {
                    OwnedTexture replacement = new OwnedTexture(PixelText.image(theme.title), true);
                    if (titleTexture != null) titleTexture.close();
                    titleTexture = replacement;
                    lastTitle = theme.title;
                }
                double tw = Math.min(w - 8.0, h * 0.48);
                double th = Math.min(18, titleTexture.height * tw / titleTexture.width);
                tw = th * titleTexture.width / titleTexture.height;
                quad(
                    titleTexture,
                    (w - tw) / 2,
                    Math.max(0, Math.min(h - th, h * theme.titleY - th / 2)),
                    tw,
                    th,
                    1,
                    theme.textColor);
            }
            int desiredWidth = theme.barWidthFraction > 0 ? Math.max(1, (int) Math.round(w * theme.barWidthFraction))
                : theme.barWidth;
            BarLayout bar = new BarLayout(w, h, desiredWidth, theme.barHeight, theme.barX, theme.barY);
            OwnedTexture track = textures == null ? null : textures.track;
            OwnedTexture fill = textures == null ? null : textures.fill;
            quad(
                track,
                bar.x,
                bar.y,
                bar.width,
                bar.height,
                1,
                track == null && !theme.track.isEmpty() && theme.trackColor == 0xFFFFFFFF ? 0xFF263441
                    : theme.trackColor);
            double fraction = progress.getFraction();
            if (fraction > 0) {
                quad(
                    fill,
                    bar.x + 2,
                    bar.y + Math.min(2, bar.height / 4.0),
                    Math.max(0, bar.width - 4) * fraction,
                    bar.height - Math.min(4, bar.height / 2.0),
                    fraction,
                    fill == null && !theme.fill.isEmpty() && theme.fillColor == 0xFFFFFFFF ? 0xFFDDB85D
                        : theme.fillColor);
            }
            if (theme.showText) {
                String count = progress.getCompleted() + " / " + progress.getTotal();
                String details = theme.showDetails ? progress.getStage() + "  " + detail : "";
                updateText(count, details, (int) bar.width);
                double ty = Math.max(0, Math.min(h - textTexture.height, bar.y + bar.height + 6));
                quad(textTexture, bar.x, ty, bar.width, textTexture.height, 1, theme.textColor);
            }
            Display.update(false);
        }
        // Pump window events without dispatching game input or recursively ticking/rendering Minecraft.
        Display.processMessages();
    }

    private void updateText(String count, String details, int width) {
        String key = count + "\n" + details + "\n" + width;
        if (key.equals(lastText) && textTexture != null) return;
        BufferedImage image = new BufferedImage(
            Math.max(1, width),
            details.isEmpty() ? 9 : 21,
            BufferedImage.TYPE_INT_ARGB);
        String label = "Reloading resources";
        if (PixelText.width(label) + PixelText.width(count) + 8 > width) label = "Reloading";
        if (PixelText.width(label) + PixelText.width(count) + 8 <= width) PixelText.draw(image, label, 0, 0);
        PixelText.draw(image, count, Math.max(0, width - PixelText.width(count)), 0);
        if (!details.isEmpty())
            PixelText.draw(image, details.substring(0, Math.min(details.length(), width / 6)), 0, 12);
        OwnedTexture replacement = new OwnedTexture(image, true);
        if (textTexture != null) textTexture.close();
        textTexture = replacement;
        lastText = key;
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
        if (titleTexture != null) {
            titleTexture.close();
            titleTexture = null;
        }
        if (textTexture != null) {
            textTexture.close();
            textTexture = null;
        }
    }
}
