package com.legacyvisualfix.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.server.integrated.IntegratedServer;

import com.google.common.hash.Hashing;
import com.legacyvisualfix.LegacyVisualFix;

/** Client-side chat state for one connection, including its local command history. */
public final class ChatSession {

    private static final int HISTORY_LIMIT = 50;
    private static final List<String> commands = new ArrayList<>();
    private static NetHandlerPlayClient connection;
    private static Path historyFile;
    private static String draft;

    private ChatSession() {}

    public static void enter(Minecraft mc) {
        NetHandlerPlayClient current = mc.getNetHandler();
        if (mc.theWorld == null || current == null || connection == current) return;

        leave(mc);
        connection = current;
        String scope = historyScope(mc, current);
        if (scope == null) return;

        String fileName = Hashing.sha256()
            .hashString(scope, StandardCharsets.UTF_8)
            .toString() + ".txt";
        historyFile = mc.mcDataDir.toPath()
            .resolve("config")
            .resolve("legacyvisualfix")
            .resolve("command-history")
            .resolve(fileName);
        readHistory();
        mc.ingameGUI.getChatGUI()
            .getSentMessages()
            .addAll(commands);
    }

    public static void leave(Minecraft mc) {
        if (connection != null && mc.ingameGUI != null) mc.ingameGUI.getChatGUI()
            .getSentMessages()
            .clear();
        connection = null;
        historyFile = null;
        commands.clear();
        draft = null;
    }

    public static String takeDraft(boolean commandOnly) {
        if (commandOnly && draft != null && !draft.startsWith("/")) return null;
        String saved = draft;
        draft = null;
        return saved;
    }

    public static void saveDraft(Minecraft mc, String text) {
        if (connection == null || connection != mc.getNetHandler() || mc.theWorld == null) return;
        String trimmed = text.trim();
        draft = trimmed.isEmpty() || trimmed.equals("/") ? null : text;
    }

    public static void record(Minecraft mc, String text) {
        enter(mc);
        if (connection == null || connection != mc.getNetHandler()
            || mc.theWorld == null
            || historyFile == null
            || !isCommand(text)) return;

        if (append(text)) writeHistory();
    }

    private static String historyScope(Minecraft mc, NetHandlerPlayClient handler) {
        IntegratedServer server = mc.getIntegratedServer();
        if (server != null) return "world:" + server.getFolderName();

        ServerData data = mc.func_147104_D();
        if (data != null && data.serverIP != null
            && !data.serverIP.trim()
                .isEmpty()) {
            return "server:" + normalizeAddress(data.serverIP);
        }

        SocketAddress address = handler.getNetworkManager()
            .getSocketAddress();
        if (address instanceof InetSocketAddress remote) {
            String host = remote.getHostString();
            if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
            return "server:" + normalizeAddress(host + ":" + remote.getPort());
        }
        return null;
    }

    private static String normalizeAddress(String address) {
        String normalized = address.trim()
            .toLowerCase(Locale.ROOT);
        int port = normalized.lastIndexOf(':');
        if (normalized.endsWith(":25565") && (port == normalized.indexOf(':') || port == normalized.indexOf(']') + 1)) {
            return normalized.substring(0, port);
        }
        return normalized;
    }

    private static boolean isCommand(String text) {
        return text.length() > 1 && text.startsWith("/") && text.indexOf('\n') < 0 && text.indexOf('\r') < 0;
    }

    private static boolean append(String command) {
        if (!commands.isEmpty() && commands.get(commands.size() - 1)
            .equals(command)) return false;
        if (commands.size() == HISTORY_LIMIT) commands.remove(0);
        commands.add(command);
        return true;
    }

    private static void readHistory() {
        try {
            if (!Files.isRegularFile(historyFile)) return;
            for (String command : Files.readAllLines(historyFile, StandardCharsets.UTF_8)) {
                if (isCommand(command)) append(command);
            }
        } catch (IOException | SecurityException e) {
            LegacyVisualFix.LOG.warn("Could not read local command history", e);
        }
    }

    private static void writeHistory() {
        Path directory = historyFile.getParent();
        try {
            Files.createDirectories(directory);
            Path temporary = Files.createTempFile(directory, "commands-", ".tmp");
            try {
                Files.write(temporary, commands, StandardCharsets.UTF_8);
                try {
                    Files.move(
                        temporary,
                        historyFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, historyFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException | SecurityException e) {
            LegacyVisualFix.LOG.warn("Could not save local command history", e);
        }
    }
}
