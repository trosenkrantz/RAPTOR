package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.RaptorService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
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
import java.util.List;
import java.util.logging.Logger;

public class WebSocketService implements RaptorService {
    private static final Logger LOGGER = Logger.getLogger(WebSocketService.class.getName());
    private static final String PARAMETER_URI = "uri";
    private static final String PARAMETER_PORT = "port";
    public static final String PARAMETER_REPLY_FILE = "reply-file";

    private static final int DEFAULT_PORT = 50000;
    private static final String DEFAULT_URI = "ws://localhost:" + DEFAULT_PORT + "/socket";

    @Override
    public String getPromptValue() {
        return "w";
    }

    @Override
    public String getParameterKey() {
        return "web-socket";
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
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("IP port of server socket", DEFAULT_PORT)));
            }
            case SERVER -> {
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("IP port of local server socket to create", DEFAULT_PORT)));
            }
        }

        TlsUtility.configureTls(configuration);

        configureSendStrategy(configuration);
    }

    private static void configureSendStrategy(Configuration configuration) throws IOException {
        ConsoleIo.write("What data to send to the remote system? ");
        SendStrategy sendStrategy = ConsoleIo.askForOptions(SendStrategy.class);
        configuration.setEnum(sendStrategy);

        if (sendStrategy.equals(SendStrategy.AUTO_REPLY)) {
            String path = ConsoleIo.askForFile("Absolute or relative file path", "." + File.separator + "replies.json");

            // Load state machine immediately to provide early feedback
            StateMachineConfiguration stateMachine = StateMachineConfiguration.readFromFile(path);
            ConsoleIo.writeLine("Parsed file with " + stateMachine.states().size() + " states and " + stateMachine.states().values().stream().map(List::size).reduce(0, Integer::sum) + " transitions.");

            configuration.setString(PARAMETER_REPLY_FILE, path);
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        WebSocketSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        sendStrategy.load(configuration);

        switch (configuration.requireEnum(Role.class)) {
            case CLIENT -> {
                String uri = configuration.requireString(PARAMETER_URI);
                WebSocketClient client = new RaptorWebSocketClient(new URI(uri), sendStrategy);
                if (configuration.requireEnum(TlsVersion.class) != TlsVersion.NONE) {
                    client.setSocketFactory(TlsUtility.loadSslContext(configuration).getSocketFactory());
                }
                LOGGER.info("Connecting to server at " + uri + "...");
                client.run(); // blocking call
            }
            case SERVER -> {
                int port = configuration.requireInt(PARAMETER_PORT);
                WebSocketServer server = new RaptorWebSocketServer(new InetSocketAddress("localhost", port), sendStrategy);
                boolean useTls = configuration.requireEnum(TlsVersion.class) != TlsVersion.NONE;
                if (useTls) server.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(TlsUtility.loadSslContext(configuration)));
                LOGGER.info("Waiting for client to connect to " + (useTls ? "wss" : "ws") + "://localhost:" + port + "...");
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
