package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.*;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.IpPortValidator;
import com.github.trosenkrantz.raptor.tls.TlsUtility;
import com.github.trosenkrantz.raptor.tls.TlsVersion;

import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocket;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TcpService implements RaptorService {
    private static final Logger LOGGER = Logger.getLogger(TcpService.class.getName());

    public static final String PARAMETER_REPLY_FILE = "reply-file";
    private static final String PARAMETER_REMOTE_HOST = "remote-host";
    private static final String PARAMETER_LOCAL_PORT = "local-port";
    private static final String PARAMETER_REMOTE_PORT = "remote-port";

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 50000;

    private static boolean shutDown;

    @Override
    public String getPromptValue() {
        return "t";
    }

    @Override
    public String getParameterKey() {
        return "tcp";
    }

    @Override
    public String getDescription() {
        return "TCP";
    }

    @Override
    public void configure(Configuration configuration) throws Exception {
        Role role = ConsoleIo.askForOptions(Role.class);
        configuration.setEnum(role);

        Void ignore = switch (role) {
            case CLIENT -> {
                configuration.setString(PARAMETER_REMOTE_HOST, ConsoleIo.askForString("Hostname / IP address of server socket to connect to", DEFAULT_HOST));
                configuration.setInt(PARAMETER_REMOTE_PORT, ConsoleIo.askForInt("Port of server socket", DEFAULT_PORT, IpPortValidator.VALIDATOR));
                ConsoleIo.askForOptionalInt(
                        "Port of local client socket",
                        "arbitrary ephemeral port",
                        IpPortValidator.VALIDATOR
                ).ifPresent(port -> configuration.setInt(PARAMETER_LOCAL_PORT, port));

                yield null;
            }
            case SERVER -> {
                configuration.setInt(PARAMETER_LOCAL_PORT, ConsoleIo.askForInt("Port of local server socket to create", DEFAULT_PORT, IpPortValidator.VALIDATOR));

                yield null;
            }
        };

        TlsUtility.configureTls(configuration);

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
            ConsoleIo.writeLine("Parsed file with " + stateMachine.states().size() + " states and " + stateMachine.states().values().stream().map(List::size).reduce(0, Integer::sum) + " transitions.");

            configuration.setString(PARAMETER_REPLY_FILE, path);
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        TcpSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        sendStrategy.load(configuration);

        try {
            Void ignore = switch (configuration.requireEnum(Role.class)) {
                case CLIENT -> {
                    try (Socket socket = getClientSocket(configuration)) {
                        runWithSocket(socket, sendStrategy);
                    }

                    yield null;
                }
                case SERVER -> {
                    int port = configuration.requireInt(PARAMETER_LOCAL_PORT);

                    try (ServerSocket socket = getServerSocket(port, configuration)) {
                        while (!shutDown) { // Open for new client when closed
                            LOGGER.info("Waiting for client to connect to port " + port + "...");
                            try {
                                runWithSocket(socket.accept(), sendStrategy);
                            } catch (SocketException e) {
                                if ("Socket closed".equals(e.getMessage())) {
                                    LOGGER.info("Socket closed normally.");
                                    // Ignore exception and connect to new client
                                } else {
                                    throw e;
                                }
                            }
                        }
                    }

                    yield null;
                }
            };
        } catch (SocketException e) {
            if ("Socket closed".equals(e.getMessage())) {
                LOGGER.info("Socket closed normally.");
                // Ignore exception
            } else {
                throw e;
            }
        }
    }

    private static Socket getClientSocket(Configuration configuration) throws Exception {
        SocketFactory factory;
        if (configuration.requireEnum(TlsVersion.class) == TlsVersion.NONE) {
            factory = SocketFactory.getDefault();
        } else {
            factory = TlsUtility.loadSslContext(configuration).getSocketFactory();
        }

        Socket socket = factory.createSocket();

        configuration.getInt(PARAMETER_LOCAL_PORT).ifPresent(port -> {
            try {
                socket.bind(new InetSocketAddress(port));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        String host = configuration.requireString(PARAMETER_REMOTE_HOST);
        int port = configuration.requireInt(PARAMETER_REMOTE_PORT);
        LOGGER.info("Connecting to server at " + host + ":" + port + "...");
        socket.connect(new InetSocketAddress(host, port));

        return socket;
    }

    private static ServerSocket getServerSocket(int port, Configuration configuration) throws Exception {
        if (configuration.requireEnum(TlsVersion.class) == TlsVersion.NONE) {
            return new ServerSocket(port);
        } else {
            SSLServerSocket socket = (SSLServerSocket) TlsUtility.loadSslContext(configuration).getServerSocketFactory().createServerSocket(port);
            socket.setWantClientAuth(true);
            return socket;
        }
    }

    private static void runWithSocket(Socket socket, TcpSendStrategy sendStrategy) throws IOException {
        LOGGER.info("Local socket at " + socket.getLocalSocketAddress() + " connected to remote socket at " + socket.getRemoteSocketAddress() + ".");

        Consumer<byte[]> onInput = sendStrategy.initialise(socket, () -> shutDown = true);

        InputStream in = socket.getInputStream();
        byte[] buffer = new byte[1024];
        int readLength;
        while ((readLength = in.read(buffer)) != -1) {
            byte[] bytesRead = new byte[readLength];
            System.arraycopy(buffer, 0, bytesRead, 0, readLength);
            LOGGER.info("Received " + BytesFormatter.bytesToFullyEscapedStringWithType(bytesRead));
            onInput.accept(bytesRead);
        }

        LOGGER.info("Socket closed normally.");
    }

}
