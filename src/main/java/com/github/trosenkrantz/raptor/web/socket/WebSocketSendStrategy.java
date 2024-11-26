package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.Configuration;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.function.Consumer;

public interface WebSocketSendStrategy {
    default void load(final Configuration configuration) throws IOException {
    }

    Consumer<byte[]> initialise(final WebSocket socket, final Runnable shutDownAction);
}
