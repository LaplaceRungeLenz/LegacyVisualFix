package com.legacyvisualfix.screenshot;

import java.awt.AWTError;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.legacyvisualfix.LegacyVisualFix;

public final class ScreenshotClipboard {

    private static final ExecutorService CLIPBOARD = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "LVF Screenshot Clipboard");
        thread.setDaemon(true);
        return thread;
    });

    private ScreenshotClipboard() {}

    public static void addCopyAction(IChatComponent result) {
        if (!(result instanceof ChatComponentTranslation message) || !"screenshot.success".equals(message.getKey())) {
            return;
        }
        Object[] args = message.getFormatArgs();
        if (args.length == 0 || !(args[0] instanceof IChatComponent fileName)) return;
        ClickEvent fileLink = fileName.getChatStyle()
            .getChatClickEvent();
        if (fileLink == null || fileLink.getAction() != ClickEvent.Action.OPEN_FILE) return;

        message.appendSibling(new ChatComponentText(" "));
        message.appendSibling(
            new ChatComponentTranslation("legacyvisualfix.screenshot.copy").setChatStyle(
                new ChatStyle().setColor(EnumChatFormatting.AQUA)
                    .setUnderlined(true)
                    .setChatClickEvent(new CopyImage(fileLink.getValue()))));
    }

    public static boolean handleClick(Minecraft mc, IChatComponent component) {
        // Only locally constructed screenshot links can perform this action.
        if (component == null || !(component.getChatStyle()
            .getChatClickEvent() instanceof CopyImage action)) {
            return false;
        }
        CLIPBOARD.execute(() -> {
            boolean copied = false;
            try {
                BufferedImage image = ImageIO.read(new File(action.getValue()));
                if (image == null) throw new IOException("Unrecognized screenshot image");
                Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new ClipboardImage(image), null);
                copied = true;
            } catch (IOException | IllegalStateException | UnsupportedOperationException | SecurityException
                | AWTError e) {
                LegacyVisualFix.LOG.warn("Could not copy screenshot to the clipboard", e);
            }
            String key = copied ? "legacyvisualfix.screenshot.copied" : "legacyvisualfix.screenshot.copyFailed";
            mc.func_152344_a(
                () -> mc.ingameGUI.getChatGUI()
                    .printChatMessage(new ChatComponentTranslation(key)));
        });
        return true;
    }

    private static final class CopyImage extends ClickEvent {

        private CopyImage(String path) {
            super(Action.OPEN_FILE, path);
        }
    }

    private static final class ClipboardImage implements Transferable {

        private final BufferedImage image;

        private ClipboardImage(BufferedImage image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return image;
        }
    }
}
