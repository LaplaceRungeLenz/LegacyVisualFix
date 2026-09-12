package com.modernnh.theme;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import com.modernnh.ModernNH;
import com.modernnh.render.OwnedTexture;

public final class ThemeTextures implements AutoCloseable {

    public final OwnedTexture background;
    public final OwnedTexture track;
    public final OwnedTexture fill;
    public final OwnedTexture logo;

    public ThemeTextures(Minecraft mc, File directory, Theme theme) {
        OwnedTexture loadedBackground = null;
        OwnedTexture loadedTrack = null;
        OwnedTexture loadedFill = null;
        OwnedTexture loadedLogo = null;
        try {
            loadedBackground = load(mc, directory, theme.background, false);
            loadedTrack = load(mc, directory, theme.track, true);
            loadedFill = load(mc, directory, theme.fill, true);
            if (theme.showLogo) loadedLogo = load(mc, directory, theme.logo, false);
        } catch (RuntimeException | LinkageError e) {
            close(loadedBackground);
            close(loadedTrack);
            close(loadedFill);
            close(loadedLogo);
            throw e;
        }
        background = loadedBackground;
        track = loadedTrack;
        fill = loadedFill;
        logo = loadedLogo;
    }

    private static OwnedTexture load(Minecraft mc, File directory, String location, boolean nearest) {
        if (location.isEmpty()) return null;
        try (InputStream input = open(mc, directory, location);
            ImageInputStream stream = new MemoryCacheImageInputStream(input)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("Not an image: " + location);
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096 || (long) width * height > 4194304) {
                    throw new IOException("Theme PNG exceeds 4096px / 4 megapixels: " + location);
                }
                BufferedImage image = reader.read(0);
                return new OwnedTexture(image, nearest);
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException e) {
            ModernNH.LOG.warn("Cannot read theme texture {}; using solid color", location, e);
            return null;
        }
    }

    private static InputStream open(Minecraft mc, File directory, String location) throws IOException {
        if (location.startsWith("file:")) {
            Path root = directory.toPath()
                .toRealPath();
            Path path = root.resolve(location.substring(5))
                .toRealPath();
            if (!path.startsWith(root)) throw new IOException("Theme file escapes config directory");
            return new FileInputStream(path.toFile());
        }
        return mc.getResourceManager()
            .getResource(new ResourceLocation(location))
            .getInputStream();
    }

    private static void close(OwnedTexture texture) {
        if (texture != null) texture.close();
    }

    @Override
    public void close() {
        close(background);
        close(track);
        close(fill);
        close(logo);
    }
}
