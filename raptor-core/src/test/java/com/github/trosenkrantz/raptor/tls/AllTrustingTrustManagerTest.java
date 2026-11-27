package com.github.trosenkrantz.raptor.tls;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

class AllTrustingTrustManagerTest {
    @Test
    void acceptsCertificateWithHostnameNotMatchingUri() throws Exception {
        String body = "data: Hello, World!\n\n";
        HttpsServer server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(TlsTestSupport.sslContext()));
        server.createContext("/events", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();

        try {
            HttpClient client = HttpClient.newBuilder().sslContext(TlsTestSupport.sslContext()).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://localhost:" + server.getAddress().getPort() + "/events"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals(body, response.body());
        } finally {
            server.stop(0);
        }
    }
}
