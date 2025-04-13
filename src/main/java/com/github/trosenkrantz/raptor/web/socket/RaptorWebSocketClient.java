package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RaptorWebSocketClient extends WebSocketClient {
    private static final Logger LOGGER = Logger.getLogger(RaptorWebSocketClient.class.getName());

    private final WebSocketSendStrategy sendStrategy;

    private Consumer<byte[]> onInput;

    public RaptorWebSocketClient(URI serverUri, WebSocketSendStrategy sendStrategy) {
        super(serverUri);
        this.sendStrategy = sendStrategy;
    }

    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm(null);
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        LOGGER.info("Local socket at " + getSocket().getLocalSocketAddress() + " connected to remote socket at " + getSocket().getRemoteSocketAddress() + ".");
        onInput = sendStrategy.initialise(this, () -> {});
    }

    @Override
    public void onMessage(String message) {
        byte[] input = message.getBytes(StandardCharsets.UTF_8); // WebSockets use UTF-8 for text frames
        LOGGER.info("Received text: " + BytesFormatter.bytesToFullyEscapedTextString(input));
        onInput.accept(input);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        byte[] input = new byte[message.remaining()];
        message.get(input);
        LOGGER.info("Received bytes: " + BytesFormatter.bytesToFullyEscapedHexString(input));
        onInput.accept(input);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info((remote ? "Remote" : "Local") + " socket closed the connection, code " + code + ", reason: " + reason);
        onInput = null;
    }

    @Override
    public void onError(Exception e) {
        LOGGER.log(Level.SEVERE, "Error occurred.", e);
    }
}
