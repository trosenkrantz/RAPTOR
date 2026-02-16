package com.github.trosenkrantz.raptor.web.socket;

import org.java_websocket.WebSocket;

import java.util.function.Consumer;

class NoneSendStrategy implements WebSocketSendStrategy {
    @Override
    public Consumer<byte[]> initialise(WebSocket socket, Runnable shutDownAction, int commandSubstitutionTimeout) {
        return input -> { // Nothing to send on inputs
        };
    }
}
