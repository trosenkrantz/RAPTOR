package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.configuration.StringToStringMapSetting;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.tls.TlsUtility;
import com.github.trosenkrantz.raptor.tls.TlsVersion;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
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
                configuration.setString(PARAMETER_URI, ConsoleIo.askForString("URI of WebSocket server endpoint to connect", DEFAULT_URI));

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

        if (sendStrategy.equals(SendStrategy.AUTO_REPLY)) {
            StateMachineConfiguration.configureSampleAutoReply(configuration, StateMachineConfiguration.REPLIES_PATH);
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        WebSocketSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        sendStrategy.load(configuration);

        switch (configuration.requireEnum(Role.class)) {
            case CLIENT -> {
                String uri = configuration.requireString(PARAMETER_URI);
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
        if (BytesFormatter.isText(userAnswerAsBytes)) { // Has only printable and control characters
            socket.send(new String(userAnswerAsBytes, StandardCharsets.US_ASCII)); // Send as text frame, using ASCII as we just checked this is ASCII text
            LOGGER.info("Sent text: " + BytesFormatter.bytesToFullyEscapedTextString(userAnswerAsBytes));
        } else {
            socket.send(userAnswerAsBytes); // Send as binary frame
            LOGGER.info("Sent bytes: " + BytesFormatter.bytesToFullyEscapedHexString(userAnswerAsBytes));
        }
    }
}
