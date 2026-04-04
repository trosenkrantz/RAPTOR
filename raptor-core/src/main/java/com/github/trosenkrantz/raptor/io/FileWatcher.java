package com.github.trosenkrantz.raptor.io;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * File watcher with basic retry mechanics.
 * <p>
 * If failing to read file, we retry once after {@link #DELAY_MS} ms.
 * <p>
 * To reduce duplicate events, we cache a checksum of the file.
 * If we detect an event with no file content change, we ignore.
 * <p>
 * If a listener fails to process the file, we retry once after {@link #DELAY_MS} ms.
 */
public final class FileWatcher implements AutoCloseable {
    public static final int DELAY_MS = 100;

    private static final Logger LOGGER = Logger.getLogger(FileWatcher.class.getName());

    private final Path path;
    private final List<Supplier<Boolean>> listeners = new CopyOnWriteArrayList<>();

    private WatchService watchService;
    private ScheduledExecutorService executor;

    private volatile FileWatcherState state;
    private volatile byte[] lastSuccessHash;

    public FileWatcher(Path path) {
        this.path = path.toAbsolutePath();
    }

    /**
     * Registers a callback triggered when the file changes.
     * We start watching on the first subscribe.
     *
     * @param listener listener, must return true iff successfully processed the file
     * @throws IOException if failing to start watching
     */
    public void subscribe(Supplier<Boolean> listener) throws IOException {
        listeners.add(listener);
        startIfNeeded();
    }

    private synchronized void startIfNeeded() throws IOException {
        if (watchService != null) return;

        watchService = FileSystems.getDefault().newWatchService();
        path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

        executor = Executors.newSingleThreadScheduledExecutor();
        state = FileWatcherState.WATCH;
        ScheduledFuture<?> unused = executor.scheduleWithFixedDelay(this::watch, 0, DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void watch() {
        try {
            if (state == FileWatcherState.WATCH) {
                WatchKey key = watchService.take();
                boolean isMyFile = IsMyFile(key);

                boolean valid = key.reset();
                if (!valid) {
                    LOGGER.warning("Can no longer watch file " + path.toAbsolutePath() + ".");
                    close();
                }

                if (!isMyFile) return; // Continue watching for files
            }

            byte[] hash;
            try {
                hash = calculateHash();
            } catch (IOException e) {
                switch (state) {
                    case WATCH -> {
                        state = FileWatcherState.RE_TRY_READ; // Try again after some time
                    }
                    case RE_TRY_READ -> {
                        LOGGER.warning("Cannot read file " + path.toAbsolutePath() + ".");
                        state = FileWatcherState.WATCH; // Give up reading for now and continue watching
                    }
                }
                return;
            }

            if (Arrays.equals(hash, lastSuccessHash)) {
                // File has not changed, continue watching
                state = FileWatcherState.WATCH;
                return;
            }

            List<Boolean> listenersSucceeded = listeners.stream().map(Supplier::get).toList(); // Notify all
            if (listenersSucceeded.stream().anyMatch(success -> !success)) { // If any listener failed reading
                switch (state) {
                    case WATCH -> { // This is a new fail
                        state = FileWatcherState.RE_TRY_READ; // A process might have done a partial write, try again after some time
                    }
                    case RE_TRY_READ -> { // This is a recurrent fail
                        LOGGER.warning("Cannot process file " + path.toAbsolutePath() + ".");
                        state = FileWatcherState.WATCH; // Give up reading for now and continue watching
                    }
                }
                lastSuccessHash = null; // We already notified listeners, reset hash to notify again if something reverts the file to something successful
                return;
            }

            lastSuccessHash = hash;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private byte[] calculateHash() throws IOException {
        byte[] content = Files.readAllBytes(this.path);
        try {
            return MessageDigest.getInstance("SHA-256").digest(content);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warning("SHA-256 hash algorithm is not available, cannot watch file " + path.toAbsolutePath() + ".");
            close();
            throw new RuntimeException(e);
        }
    }

    private boolean IsMyFile(WatchKey key) {
        return key.pollEvents().stream()
                .map(event -> (Path) event.context())
                .anyMatch(path -> path != null && path.equals(this.path.getFileName()));
    }

    @Override
    public void close() {
        if (executor != null) executor.shutdownNow();
        try {
            if (watchService != null) watchService.close();
        } catch (IOException ignore) {
            // Ignore
        }
    }
}
