package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RaptorWebSocketServer extends WebSocketServer {
    private static final Logger LOGGER = Logger.getLogger(RaptorWebSocketServer.class.getName());

    private final WebSocketSendStrategy sendStrategy;
    private final Map<InetSocketAddress, Consumer<byte[]>> onInput = new HashMap<>();

    public RaptorWebSocketServer(InetSocketAddress address, WebSocketSendStrategy sendStrategy) {
        super(address);
        this.sendStrategy = sendStrategy;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        LOGGER.info("Local socket at " + webSocket.getLocalSocketAddress() + " connected to remote socket at " + webSocket.getRemoteSocketAddress() + ".");
        onInput.put(webSocket.getRemoteSocketAddress(), sendStrategy.initialise(webSocket, () -> {
            try {
                stop();
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Error stopping server.", e);
                Thread.currentThread().interrupt();
            }
        }));
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        byte[] input = message.getBytes(StandardCharsets.UTF_8); // WebSockets use UTF-8 for text frames
        InetSocketAddress remoteAddress = webSocket.getRemoteSocketAddress();
        LOGGER.info("Received text from " + remoteAddress + ": " + BytesFormatter.bytesToFullyEscapedTextString(input));
        onInput.get(remoteAddress).accept(input);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteBuffer message) {
        byte[] input = new byte[message.remaining()];
        message.get(input);
        InetSocketAddress remoteAddress = webSocket.getRemoteSocketAddress();
        LOGGER.info("Received bytes from " + remoteAddress + ": " + BytesFormatter.bytesToFullyEscapedHexString(input));
        onInput.get(remoteAddress).accept(input);
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        LOGGER.info((remote ? "Remote" : "Local") + " socket closed the connection, code " + code + ", reason: " + reason);
        onInput.remove(webSocket.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        LOGGER.log(Level.SEVERE, "Error occurred.", e);
    }
}
