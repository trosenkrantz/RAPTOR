package com.github.trosenkrantz.raptor.configuration;

import java.io.IOException;

public record ReloadableConfiguration(Configuration configuration, FileWatcher watcher) implements AutoCloseable {
    @Override
    public void close() throws IOException {
        watcher.close();
    }
}