package com.github.trosenkrantz.raptor.configuration;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FileWatcher implements AutoCloseable {
    private static final long DEBOUNCE_MS = 100;

    private final Path path;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private WatchService watchService;
    private ExecutorService executor;

    private volatile long lastTrigger = 0;

    public FileWatcher(Path path) {
        this.path = path.toAbsolutePath();
    }

    /**
     * Registers a callback triggered when the file changes.
     * Infrastructure starts lazily on first subscription.
     */
    public void subscribe(Runnable listener) throws IOException {
        listeners.add(listener);
        startIfNeeded();
    }

    private synchronized void startIfNeeded() throws IOException {
        if (watchService != null) return;

        watchService = FileSystems.getDefault().newWatchService();
        path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

        executor = Executors.newSingleThreadExecutor();
        executor.execute(this::loop);
    }

    private void loop() {
        try {
            boolean valid = true;
            while (valid) {
                WatchKey key = watchService.take();

                boolean relevant = key.pollEvents().stream()
                        .map(event -> (Path) event.context())
                        .anyMatch(path -> path != null && path.equals(this.path.getFileName()));
                if (relevant) {
                    long now = System.currentTimeMillis();
                    if (now - lastTrigger > DEBOUNCE_MS) {
                        lastTrigger = now;
                        listeners.forEach(Runnable::run);
                    }
                }

                valid = key.reset();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() throws IOException {
        if (executor != null) executor.shutdownNow();
        if (watchService != null) watchService.close();
    }
}
