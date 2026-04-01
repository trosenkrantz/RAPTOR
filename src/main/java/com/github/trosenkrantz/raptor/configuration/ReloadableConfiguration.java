package com.github.trosenkrantz.raptor.configuration;

import com.github.trosenkrantz.raptor.io.FileWatcher;

public record ReloadableConfiguration(Configuration configuration, FileWatcher watcher) implements AutoCloseable {
    @Override
    public void close() {
        watcher.close();
    }
}