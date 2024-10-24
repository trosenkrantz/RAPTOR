package com.github.trosenkrantz.raptor.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.PromptOption;
import com.github.trosenkrantz.raptor.RaptorService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachine;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class TcpService implements RaptorService {
    private static final Logger LOGGER = Logger.getLogger(TcpService.class.getName());

    public static final String DEFAULT_ADDRESS = "localhost";
    public static final int DEFAULT_PORT = 50000;
    public static final String PARAMETER_SEND_FILE = "send-file";
    public static final String PARAMETER_SEND_STRATEGY = "send-strategy";
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
    public void configure(Configuration configuration) {
        TcpRole tcpRole = ConsoleIo.askFor(TcpRole.getPromptOptions());
        configuration.setEnum(tcpRole);

        Void ignore = switch (tcpRole) {
            case CLIENT -> {
                String address = ConsoleIo.askForString("IP address of server socket to connect to", DEFAULT_ADDRESS);
                configuration.setString("address", address);

                int port = ConsoleIo.askForInt("IP port of server socket", DEFAULT_PORT);
                configuration.setString("port", String.valueOf(port));

                yield null;
            }
            case SERVER -> {
                int port = ConsoleIo.askForInt("IP port of local server socket to create", DEFAULT_PORT);
                configuration.setString("port", String.valueOf(port));

                yield null;
            }
        };

        configureWhatToSend(configuration);
    }

    private static void configureWhatToSend(Configuration configuration) {
        ConsoleIo.write("What data to send to the remote system? ");
        TcpSendFromOption whatToSendType = ConsoleIo.askFor(List.of(
                new PromptOption<>("n", "Do [n]ot send", TcpSendFromOption.NONE),
                new PromptOption<>("u", "Prompt over console interactively, reading as [U]TF-8", TcpSendFromOption.CONSOLE_UTF_8),
                new PromptOption<>("h", "Prompt over console interactively, reading as [h]ex", TcpSendFromOption.CONSOLE_HEX),
                new PromptOption<>("f", "Send content of a [f]ile", TcpSendFromOption.FILE),
                new PromptOption<>("a", "Configure an [a]uto-reply", TcpSendFromOption.AUTO_REPLY)
        ));
        configuration.setString(PARAMETER_SEND_STRATEGY, whatToSendType.toString());

        if (whatToSendType.equals(TcpSendFromOption.FILE)) {
            String path = ConsoleIo.askForFile("Absolute or relative file path", "." + File.separator + "out");
            // Read file immediately to provide early feedback
            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(configuration.requireString(PARAMETER_SEND_FILE)));
                ConsoleIo.writeLine("Read file with " + fileContent.length + " bytes.");
            } catch (IOException e) {
                ConsoleIo.writeLine("Failed reading file.");
                throw new UncheckedIOException(e);
            }

            configuration.setString(PARAMETER_SEND_FILE, path);
        }

        if (whatToSendType.equals(TcpSendFromOption.AUTO_REPLY)) {
            String path = ConsoleIo.askForFile("Absolute or relative file path", "." + File.separator + "reply.json");

            // Load state machine immediately to provide early feedback
            try {
                StateMachineConfiguration stateMachine = new ObjectMapper().readValue(new File(path), StateMachineConfiguration.class);
                ConsoleIo.writeLine("Parsed file with " + stateMachine.states().keySet().size() + " states and " + stateMachine.states().values().stream().map(List::size).reduce(0, Integer::sum) + " transitions.");
            } catch (IOException e) {
                ConsoleIo.writeLine("Failed reading file.");
                throw new UncheckedIOException(e);
            }

            configuration.setString(PARAMETER_SEND_FILE, path);
        }
    }

    @Override
    public void run(Configuration configuration) {
        TcpSendStrategy sendStrategy;
        try {
            sendStrategy = loadSendStrategy(configuration);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try {
            Void ignore = switch (configuration.requireEnum(TcpRole.class)) {
                case CLIENT -> {
                    String address = configuration.requireString("address");
                    int port = configuration.requireInt("port");

                    LOGGER.info("Connecting to server at " + address + ":" + port + "...");
                    try (Socket socket = new Socket(address, port)) {
                        runWithSocket(socket, sendStrategy);
                    } catch (UnknownHostException e) {
                        throw new IllegalStateException("Server not found.", e);
                    }

                    yield null;
                }
                case SERVER -> {
                    int port = configuration.requireInt("port");

                    try (ServerSocket socket = new ServerSocket(port)) {
                        while (!shutDown) { // Open for new client when closed
                            LOGGER.info("Waiting for client to connect to port " + port + "...");
                            try {
                                runWithSocket(socket.accept(), sendStrategy);
                                LOGGER.info("Socket closed normally.");
                            } catch (SocketException e) {
                                if ("Socket closed".equals(e.getMessage())) {
                                    LOGGER.info("Socket closed normally.");
                                    // Ignore exception
                                } else {
                                    throw new UncheckedIOException("I/O error.", e);
                                }
                            }
                        }
                    }

                    yield null;
                }
            };
            LOGGER.info("Socket closed normally.");
        } catch (IOException e) {
            if (e instanceof SocketException && "Socket closed".equals(e.getMessage())) {
                LOGGER.info("Socket closed normally.");
                // Ignore exception
            } else {
                throw new UncheckedIOException("I/O error.", e);
            }
        }
    }

    private TcpSendStrategy loadSendStrategy(Configuration configuration) throws IOException {
        TcpSendFromOption sendFrom = configuration.requireEnum(PARAMETER_SEND_STRATEGY, TcpSendFromOption.class);

        return switch (sendFrom) {
            case NONE -> socket -> { // Nothing to send initially
                return input -> { // Nothing to send on inputs
                };
            };
            case CONSOLE_UTF_8 -> socket -> {
                keepAskingUserWhatToSend(
                        socket,
                        () -> ConsoleIo.askForString("What to send", "Hello, World!")
                                .getBytes(StandardCharsets.UTF_8)
                );
                return input -> { // Nothing to send on inputs
                };
            };
            case CONSOLE_HEX -> {
                HexFormat hexFormat = HexFormat.of();
                AtomicBoolean firstTime = new AtomicBoolean(true);
                yield socket -> {
                    keepAskingUserWhatToSend(
                            socket,
                            () -> {
                                String whatToSend = "What to send";
                                if (firstTime.get()) {
                                    whatToSend += ", case insensitive, space delimiter optional";
                                    firstTime.set(false);
                                }
                                return hexFormat.parseHex(
                                        ConsoleIo.askForString(whatToSend, "48 69").toLowerCase()
                                );
                            }
                    );
                    return input -> { // Nothing to send on inputs
                    };
                };
            }
            case FILE -> {
                // Read file immediately to provide early feedback
                byte[] fileContentToSend = Files.readAllBytes(Paths.get(configuration.requireString(PARAMETER_SEND_FILE)));

                yield socket -> {
                    socket.getOutputStream().write(fileContentToSend);
                    LOGGER.info("Sent " + BytesFormatter.format(fileContentToSend));
                    return input -> { // Nothing to send on inputs
                    };
                };
            }
            case AUTO_REPLY -> {
                // Read state machine immediately to provide early feedback
                StateMachineConfiguration stateMachineConfiguration;
                stateMachineConfiguration = new ObjectMapper().readValue(new File(configuration.requireString(PARAMETER_SEND_FILE)), StateMachineConfiguration.class);

                yield socket -> {
                    OutputStream out = socket.getOutputStream();
                    StateMachine stateMachine = new StateMachine(stateMachineConfiguration, output -> {
                        try {
                            out.write(output);
                            LOGGER.info("Sent " + BytesFormatter.format(output));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                    return stateMachine::onInput;
                };
            }
        };
    }

    private static void keepAskingUserWhatToSend(Socket socket, Supplier<byte[]> supplier) {
        Thread.ofVirtual().start(() -> {
                    try {
                        OutputStream out = socket.getOutputStream();
                        byte[] whatToSends = supplier.get();
                        while (!socket.isInputShutdown()) {
                            out.write(whatToSends);
                            LOGGER.info("Sent " + BytesFormatter.format(whatToSends));

                            whatToSends = supplier.get();
                        }
                    } catch (AbortedException ignore) {
                        shutDown = true;
                    } catch (Exception e) {
                        ConsoleIo.writeException(e);
                        shutDown = true;
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            ConsoleIo.writeException(e);
                            shutDown = true;
                        }
                    }
                }
        );
    }

    private static void runWithSocket(Socket socket, TcpSendStrategy sendStrategy) throws IOException {
        LOGGER.info("Local socket at " + socket.getLocalSocketAddress() + " connected to remote socket at " + socket.getRemoteSocketAddress() + ".");

        Consumer<byte[]> onInput = sendStrategy.initialise(socket);

        InputStream in = socket.getInputStream();
        byte[] buffer = new byte[1024];
        int readLength;
        while ((readLength = in.read(buffer)) != -1) {
            byte[] bytesRead = new byte[readLength];
            System.arraycopy(buffer, 0, bytesRead, 0, readLength);
            LOGGER.info("Received " + BytesFormatter.format(bytesRead));
            onInput.accept(bytesRead);
        }
    }
}
