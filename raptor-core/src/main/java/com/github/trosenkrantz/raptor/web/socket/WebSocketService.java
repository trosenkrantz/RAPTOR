package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.configuration.StringToStringMapSetting;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.logging.CaptureLogger;
import com.github.trosenkrantz.raptor.tls.TlsUtility;
import com.github.trosenkrantz.raptor.tls.TlsVersion;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class WebSocketService implements RootService {
    private static final Logger LOGGER = Logger.getLogger(WebSocketService.class.getName());
    private static final String PARAMETER_URI = "uri";
    private static final String PARAMETER_PORT = "port";

    private static final int DEFAULT_PORT = 50000;
    private static final String DEFAULT_URI = "ws://localhost:" + DEFAULT_PORT + "/socket";

    public static final StringToStringMapSetting EXTRA_HEADERS_SETTING = new StringToStringMapSetting.Builder("h", "headers", "Extra HTTP headers", "Extra HTTP headers to include in the WebSocket handshake request")
            .build();

    @Override
    public String getPromptValue() {
        return "w";
    }

    @Override
    public String getParameterKey() {
        return "webSocket";
    }

    @Override
    public String getDescription() {
        return "WebSocket";
    }

    @Override
    public void configure(Configuration configuration) throws Exception {
        Role role = ConsoleIo.askForOptions(Role.class);
        configuration.setEnum(role);

        switch (role) {
            case CLIENT -> {
                configuration.setRaptorEncodedString(PARAMETER_URI, ConsoleIo.askForString("URI of WebSocket server endpoint to connect", DEFAULT_URI));

                EXTRA_HEADERS_SETTING.configure(configuration);

                TlsUtility.configureTls(configuration, false);
            }
            case SERVER -> {
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("IP port of local server socket to create", DEFAULT_PORT));

                TlsUtility.configureTls(configuration, true);
            }
        }

        configureSendStrategy(configuration);
    }

    private static void configureSendStrategy(Configuration configuration) throws IOException {
        ConsoleIo.write("What data to send to the remote system? ");
        SendStrategy sendStrategy = ConsoleIo.askForOptions(SendStrategy.class);
        configuration.setEnum(sendStrategy);

        sendStrategy.getStrategy().configure(configuration);
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        WebSocketSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        sendStrategy.load(configuration);

        switch (configuration.requireEnum(Role.class)) {
            case CLIENT -> {
                String uri = configuration.requireRaptorEncodedString(PARAMETER_URI);
                Map<String, String> extraHeaders = EXTRA_HEADERS_SETTING.read(configuration).orElse(new HashMap<>());
                WebSocketClient client = new RaptorWebSocketClient(new URI(uri), sendStrategy, extraHeaders);
                if (configuration.requireEnum(TlsVersion.class) != TlsVersion.NONE) {
                    client.setSocketFactory(TlsUtility.loadSslContext(configuration).getSocketFactory());
                }
                LOGGER.info("Connecting to server at " + uri + "...");
                client.run(); // blocking call
            }
            case SERVER -> {
                int port = configuration.requireInt(PARAMETER_PORT);
                WebSocketServer server = new RaptorWebSocketServer(new InetSocketAddress("0.0.0.0", port), sendStrategy);
                boolean useTls = configuration.requireEnum(TlsVersion.class) != TlsVersion.NONE;
                if (useTls) server.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(TlsUtility.loadSslContext(configuration)));
                LOGGER.info("Waiting for client to connect to " + (useTls ? "wss" : "ws") + "://0.0.0.0:" + port + "...");
                server.run(); // blocking call
            }
        }
    }

    public static void send(WebSocket socket, byte[] userAnswerAsBytes) {
        sendFrame(socket, userAnswerAsBytes, BytesFormatter.isText(userAnswerAsBytes));
    }

    /** Sends a frame with an explicit text/binary choice. */
    public static void sendFrame(WebSocket socket, byte[] data, boolean text) {
        sendFrame(socket, data, text, Map.of());
    }

    /** Sends a frame with an explicit text/binary choice and additional capture metadata. */
    public static void sendFrame(WebSocket socket, byte[] data, boolean text, Map<String, String> metadata) {
        if (text) {
            socket.send(decodeUtf8(data));
        } else {
            socket.send(data);
        }
        Map<String, String> allMetadata = new java.util.LinkedHashMap<>(metadata);
        allMetadata.put("frame", text ? "text" : "binary");
        CaptureLogger.sent(data, allMetadata);
    }

    private static String decodeUtf8(byte[] data) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("WebSocket text frames require valid UTF-8 data.", e);
        }
    }
}
