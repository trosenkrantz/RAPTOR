package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.RaptorService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.server.WebSocketServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

public class WebSocketService implements RaptorService {
    private static final Logger LOGGER = Logger.getLogger(WebSocketService.class.getName());
    private static final String PARAMETER_HOST = "host";
    private static final String PARAMETER_PORT = "port";
    public static final String PARAMETER_SEND_FILE = "send-file";

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 50000;

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

        Void ignore = switch (role) {
            case CLIENT -> {
                configuration.setString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of server socket to connect to", DEFAULT_HOST));
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("IP port of server socket", DEFAULT_PORT)));

                yield null;
            }
            case SERVER -> {
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("IP port of local server socket to create", DEFAULT_PORT)));

                yield null;
            }
        };

        configureWhatToSend(configuration);
    }

    private static void configureWhatToSend(Configuration configuration) throws IOException {
        ConsoleIo.write("What data to send to the remote system? ");
        SendStrategy sendStrategy = ConsoleIo.askForOptions(SendStrategy.class);
        configuration.setEnum(sendStrategy);

        if (sendStrategy.equals(SendStrategy.AUTO_REPLY)) {
            String path = ConsoleIo.askForFile("Absolute or relative file path", "." + File.separator + "tcp-replies.json");

            // Load state machine immediately to provide early feedback
            StateMachineConfiguration stateMachine = StateMachineConfiguration.readFromFile(path);
            ConsoleIo.writeLine("Parsed file with " + stateMachine.states().keySet().size() + " states and " + stateMachine.states().values().stream().map(List::size).reduce(0, Integer::sum) + " transitions.");

            configuration.setString(PARAMETER_SEND_FILE, path);
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        WebSocketSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        sendStrategy.load(configuration);

        Void ignore = switch (configuration.requireEnum(Role.class)) {
            case CLIENT -> {
                String host = configuration.requireString(PARAMETER_HOST);
                int port = configuration.requireInt(PARAMETER_PORT);
                WebSocketClient client = new RaptorWebSocketClient(new URI("ws://" + host + ":" + port), sendStrategy);
                LOGGER.info("Connecting to server at " + host + ":" + port + "...");
                client.run(); // blocking call

                yield null;
            }
            case SERVER -> {
                int port = configuration.requireInt(PARAMETER_PORT);
                WebSocketServer server = new RaptorWebSocketServer(new InetSocketAddress("localhost", port), sendStrategy);
                LOGGER.info("Waiting for client to connect to port " + port + "...");
                server.run(); // blocking call

                yield null;
            }
        };
    }

    public static void send(WebSocket socket, byte[] userAnswerAsBytes) {
        if (BytesFormatter.isText(userAnswerAsBytes)) { // Has only printable and control characters
            socket.send(new String(userAnswerAsBytes)); // Send as text frame
            LOGGER.info("Sent text: " + BytesFormatter.bytesToFullyEscapedTextString(userAnswerAsBytes));
        } else {
            socket.send(userAnswerAsBytes); // Send as binary frame
            LOGGER.info("Sent bytes: " + BytesFormatter.bytesToFullyEscapedHexString(userAnswerAsBytes));
        }
    }
}
