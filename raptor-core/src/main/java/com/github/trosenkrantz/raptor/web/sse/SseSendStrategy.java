package com.github.trosenkrantz.raptor.web.sse;

import com.github.trosenkrantz.raptor.configuration.Configuration;

import java.io.IOException;

public interface SseSendStrategy {
    default void configure(final Configuration configuration) throws IOException {
    }

    /**
     * To be called once when running the service.
     *
     * @param configuration configuration to load
     */
    default void load(final Configuration configuration) {
    }

    /**
     * To be called once when the server starts.
     *
     * @param server         server to send events through
     * @param shutDownAction action to call when the service should shut down, e.g., if aborted by the user
     */
    void start(final SseServer server, final Runnable shutDownAction);
}
