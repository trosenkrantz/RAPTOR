package com.github.trosenkrantz.raptor.web.socket;

import org.java_websocket.WebSocket;

import java.util.function.Consumer;

public interface WebSocketSendStrategy {
    Consumer<byte[]> initialise(final WebSocket socket);
}
