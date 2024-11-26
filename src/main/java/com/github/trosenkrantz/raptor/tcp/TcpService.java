package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.*;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TcpService implements RaptorService {
    private static final Logger LOGGER = Logger.getLogger(TcpService.class.getName());

    public static final String PARAMETER_SEND_FILE = "send-file";
    private static final String PARAMETER_HOST = "host";
    private static final String PARAMETER_PORT = "port";
    private static final String PARAMETER_KEY_STORE = "key-store";
    private static final String PARAMETER_KEY_STORE_PASSWORD = "key-store-password";
    private static final String PARAMETER_KEY_PASSWORD = "key-password";

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
                configuration.setString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of server socket to connect to", DEFAULT_HOST));
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("IP port of server socket", DEFAULT_PORT)));

                yield null;
            }
            case SERVER -> {
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("IP port of local server socket to create", DEFAULT_PORT)));

                yield null;
            }
        };

        configureTls(configuration);

        configureWhatToSend(configuration);
    }

    private void configureTls(Configuration configuration) {
        TlsVersion tlsVersion = ConsoleIo.askForOptions(TlsVersion.class, TlsVersion.NONE);
        configuration.setEnum(tlsVersion);
        if (tlsVersion != TlsVersion.NONE) {
            String keyStorePath = ConsoleIo.askForFile("Absolute or relate path to key store (PKCS #12 or JKS)", "." + File.separator + "KeyStore.p12");
            configuration.setString(PARAMETER_KEY_STORE, keyStorePath);

            String keyStorePassword = ConsoleIo.askForString("Password of key store", pw -> {
                try {
                    loadKeyStore(keyStorePath, pw); // Validate by trying to load
                    return Optional.empty();
                } catch (Exception e) {
                    return Optional.of("Failed loading key store with password. " + e.getMessage());
                }
            });
            configuration.setString(PARAMETER_KEY_STORE_PASSWORD, keyStorePassword);

            configuration.setString(
                    PARAMETER_KEY_PASSWORD,
                    ConsoleIo.askForString("Password of key", keyStorePassword, pw -> {
                        try {
                            loadKey(keyStorePath, keyStorePassword, pw, false); // Validate by trying to load
                            return Optional.empty();
                        } catch (Exception e) {
                            return Optional.of("Failed loading key with password. " + e.getMessage());
                        }
                    })
            );
        }
    }

    private static KeyStore loadKeyStore(String path, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(determineKeyStoreType(path));
        try (FileInputStream keyStoreStream = new FileInputStream(path)) {
            keyStore.load(keyStoreStream, password.toCharArray());
        }
        return keyStore;
    }

    private static KeyManagerFactory loadKey(String path, String keyStorePassword, String keyPassword, boolean logCerts) throws Exception {
        KeyStore keyStore = loadKeyStore(path, keyStorePassword);
        if (logCerts) {
            List<Certificate> certificates = Collections.list(keyStore.aliases()).stream().flatMap(alias -> {
                try {
                    return Arrays.stream(keyStore.getCertificateChain(alias));
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
            LOGGER.info("Using " + certificates.size() + " certificates: " + certificates);
        }

        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore, keyPassword.toCharArray());
        return factory;
    }

    private static SSLContext loadSslContext(Configuration configuration) throws Exception {
        KeyManagerFactory factory = loadKey(
                configuration.requireString(PARAMETER_KEY_STORE),
                configuration.requireString(PARAMETER_KEY_STORE_PASSWORD),
                configuration.requireString(PARAMETER_KEY_PASSWORD),
                true
        );

        SSLContext sslContext = SSLContext.getInstance(configuration.requireEnum(TlsVersion.class).getId());
        sslContext.init(factory.getKeyManagers(), new TrustManager[]{new AllTrustingTrustManager()}, new SecureRandom());

        return sslContext;
    }

    private static String determineKeyStoreType(String keyStorePath) {
        if (keyStorePath.toLowerCase().endsWith(".p12") || keyStorePath.toLowerCase().endsWith(".pfx")) {
            return "PKCS12";
        } else {
            return "JKS";
        }
    }

    private static void configureWhatToSend(Configuration configuration) throws IOException {
        ConsoleIo.write("What data to send to the remote system? ");
        SendStrategy sendStrategy = ConsoleIo.askForOptions(SendStrategy.class);
        configuration.setEnum(sendStrategy);

        if (sendStrategy.equals(SendStrategy.FILE)) {
            String path = ConsoleIo.askForFile("Absolute or relative file path", "." + File.separator + "out");
            // Read file immediately to provide early feedback
            ConsoleIo.writeLine("Read file with " + Files.readAllBytes(Paths.get(path)).length + " bytes.");

            configuration.setString(PARAMETER_SEND_FILE, path);
        }

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
        TcpSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        sendStrategy.load(configuration);

        try {
            Void ignore = switch (configuration.requireEnum(Role.class)) {
                case CLIENT -> {
                    String host = configuration.requireString(PARAMETER_HOST);
                    int port = configuration.requireInt(PARAMETER_PORT);

                    LOGGER.info("Connecting to server at " + host + ":" + port + "...");
                    try (Socket socket = getClientSocket(host, port, configuration)) {
                        runWithSocket(socket, sendStrategy);
                    }

                    yield null;
                }
                case SERVER -> {
                    int port = configuration.requireInt(PARAMETER_PORT);

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

    private static Socket getClientSocket(String host, int port, Configuration configuration) throws Exception {
        if (configuration.requireEnum(TlsVersion.class) == TlsVersion.NONE) {
            return new Socket(host, port);
        } else {
            return loadSslContext(configuration).getSocketFactory().createSocket(host, port);
        }
    }

    private static ServerSocket getServerSocket(int port, Configuration configuration) throws Exception {
        if (configuration.requireEnum(TlsVersion.class) == TlsVersion.NONE) {
            return new ServerSocket(port);
        } else {
            SSLServerSocket socket = (SSLServerSocket) loadSslContext(configuration).getServerSocketFactory().createServerSocket(port);
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
