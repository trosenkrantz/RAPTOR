package com.github.trosenkrantz.raptor.sse;

import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.StringToStringMapSetting;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.IpPortValidator;
import com.github.trosenkrantz.raptor.tls.TlsUtility;
import com.github.trosenkrantz.raptor.tls.TlsVersion;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ServerSentEventsService implements RootService {
    private static final Logger LOGGER = Logger.getLogger(ServerSentEventsService.class.getName());
    private static final String PARAMETER_URI = "uri";
    private static final String PARAMETER_PORT = "port";

    private static final int DEFAULT_PORT = 50000;
    private static final String DEFAULT_PATH = "/events";

    public static final StringToStringMapSetting EXTRA_HEADERS_SETTING = new StringToStringMapSetting.Builder(
            "h", "headers", "Extra HTTP headers", "Extra HTTP headers to include in the SSE request")
            .build();

    @Override
    public String getPromptValue() {
        return "sse";
    }

    @Override
    public String getParameterKey() {
        return "sse";
    }

    @Override
    public String getDescription() {
        return "Server-Sent Events";
    }

    @Override
    public void configure(Configuration configuration) throws Exception {
        Role role = ConsoleIo.askForOptions(Role.class);
        configuration.setEnum(role);

        switch (role) {
            case CLIENT -> {
                TlsUtility.configureTls(configuration, false);
                TlsVersion tlsVersion = configuration.requireEnum(TlsVersion.class);
                String scheme = tlsVersion == TlsVersion.NONE ? "http" : "https";
                String defaultUri = scheme + "://localhost:" + DEFAULT_PORT + DEFAULT_PATH;
                configuration.setRaptorEncodedString(PARAMETER_URI, ConsoleIo.askForString("URI of SSE endpoint to connect", defaultUri));

                EXTRA_HEADERS_SETTING.configure(configuration);
            }
            case SERVER -> {
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("IP port of local server socket to create", DEFAULT_PORT, IpPortValidator.VALIDATOR));

                ConsoleIo.write("What data to send to the remote system? ");
                SendStrategy sendStrategy = ConsoleIo.askForOptions(SendStrategy.class);
                configuration.setEnum(sendStrategy);
                sendStrategy.getStrategy().configure(configuration);

                TlsUtility.configureTls(configuration, true);
            }
        }
    }

    @Override
    public void run(Configuration configuration) throws Exception {
        switch (configuration.requireEnum(Role.class)) {
            case CLIENT -> runClient(configuration);
            case SERVER -> runServer(configuration);
        }
    }

    static void validateUriScheme(URI uri, TlsVersion tlsVersion) {
        boolean useTls = tlsVersion != TlsVersion.NONE;
        String expectedScheme = useTls ? "https" : "http";
        if (!expectedScheme.equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("URI scheme must be " + expectedScheme + ":// when TLS version is " + tlsVersion.getConfigurationId() + ": " + uri);
        }
    }

    private static void runClient(Configuration configuration) throws Exception {
        URI uri = new URI(configuration.requireRaptorEncodedString(PARAMETER_URI));
        TlsVersion tlsVersion = configuration.requireEnum(TlsVersion.class);
        validateUriScheme(uri, tlsVersion);
        Map<String, String> extraHeaders = EXTRA_HEADERS_SETTING.read(configuration).orElse(new HashMap<>());

        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1); // The bundled SSE server speaks HTTP/1.1 only
        if (tlsVersion != TlsVersion.NONE) {
            clientBuilder.sslContext(TlsUtility.loadSslContext(configuration));
            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm(null); // Accept self-signed certificates
            clientBuilder.sslParameters(sslParameters);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .header("Accept", "text/event-stream")
                .GET();
        extraHeaders.forEach(requestBuilder::setHeader);

        HttpResponse.BodyHandler<Void> bodyHandler = responseInfo -> {
            if (responseInfo.statusCode() == 200) {
                LOGGER.info("Connected to server at " + uri + ".");
            } else {
                LOGGER.warning("Server responded with HTTP " + responseInfo.statusCode() + ".");
            }
            return new SseEventSubscriber(payload -> LOGGER.info("Received " + BytesFormatter.bytesToRaptorEncodingWithType(payload)));
        };

        LOGGER.info("Connecting to server at " + uri + "...");
        clientBuilder.build().sendAsync(requestBuilder.build(), bodyHandler).join();
    }

    private static void runServer(Configuration configuration) throws Exception {
        SseSendStrategy sendStrategy = configuration.requireEnum(SendStrategy.class).getStrategy();
        sendStrategy.load(configuration);

        int port = configuration.requireInt(PARAMETER_PORT);
        boolean useTls = configuration.requireEnum(TlsVersion.class) != TlsVersion.NONE;
        SSLContext sslContext = useTls ? TlsUtility.loadSslContext(configuration) : null;

        SseServer server = new SseServer(port, sslContext);
        LOGGER.info("Waiting for client to connect to " + (useTls ? "https" : "http") + "://0.0.0.0:" + port + DEFAULT_PATH + "...");
        sendStrategy.start(server, server::close);
        server.run();
    }
}
