package com.legacyvisualfix.resourcepack;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.apache.commons.io.IOUtils;

public final class PackDirectoryWatcher implements Closeable {

    private final Path directory;
    private final WatchService service;
    private final WatchKey rootKey;

    public PackDirectoryWatcher(Path directory) throws IOException {
        this.directory = directory;
        service = directory.getFileSystem()
            .newWatchService();
        try {
            rootKey = register(directory);
            registerPackFolders();
        } catch (IOException | RuntimeException e) {
            IOUtils.closeQuietly(service);
            throw e;
        }
    }

    public boolean pollForChanges() throws IOException {
        boolean changed = false;
        WatchKey key;
        while ((key = service.poll()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                changed = true;
                if (key == rootKey) {
                    if (event.kind() == ENTRY_CREATE) {
                        registerPackFolder(directory.resolve((Path) event.context()));
                    } else if (event.kind() == OVERFLOW) {
                        registerPackFolders();
                    }
                }
            }
            key.reset();
        }
        if (!rootKey.isValid()) throw new IOException("Resource pack directory is no longer watchable: " + directory);
        return changed;
    }

    private void registerPackFolders() throws IOException {
        try (DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
            for (Path child : children) registerPackFolder(child);
        } catch (DirectoryIteratorException e) {
            throw e.getCause();
        }
    }

    private void registerPackFolder(Path child) throws IOException {
        // A folder copied in stages may receive pack.mcmeta after the root event has been processed.
        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
            try {
                register(child);
            } catch (NoSuchFileException | NotDirectoryException ignored) {
                // The folder was removed or replaced before it could be registered.
            }
        }
    }

    private WatchKey register(Path path) throws IOException {
        return path.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    }

    @Override
    public void close() throws IOException {
        service.close();
    }
}
