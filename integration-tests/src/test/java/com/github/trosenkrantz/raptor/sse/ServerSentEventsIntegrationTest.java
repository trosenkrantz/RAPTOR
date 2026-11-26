package com.github.trosenkrantz.raptor.sse;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ServerSentEventsIntegrationTest extends RaptorIntegrationTest {
    @Test
    public void interactive() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor client = new Raptor(network)) {
            network.startAll();

            server.runConfiguration("""
                    {
                      "service": "sse",
                      "role": "server",
                      "port": 50000,
                      "sendStrategy": "interactive",
                      "commandSubstitutionTimeout": 1000,
                      "tlsVersion": "none"
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Waiting for client to connect");

            client.runConfiguration(String.format("""
                    {
                      "service": "sse",
                      "role": "client",
                      "uri": "http://%s:50000/events",
                      "tlsVersion": "none"
                    }
                    """, server.getRaptorHostname()));
            server.expectNumberOfOutputLineContains(1, "connected", client.getRaptorIpAddress());
            client.expectNumberOfOutputLineContains(1, "connected to server at");

            String textMessage = "Hello, World!";
            sendEvent(server, textMessage, "", "");
            server.expectNumberOfOutputLineContains(1, "sent", textMessage);
            client.expectNumberOfOutputLineContains(1, "received", textMessage);

            String newLineMessage = "\\n"; // RAPTOR encoding for a line feed
            sendEvent(server, newLineMessage, "", "");
            client.expectNumberOfOutputLineContains(1, "received", "\\n");

            String metadataMessage = "With metadata";
            sendEvent(server, metadataMessage, "42", "update");
            client.expectNumberOfOutputLineContains(1, "received", "id=\"42\"", "event=\"update\"", metadataMessage);
        }
    }

    @Test
    public void multipleClients() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor client1 = new Raptor(network);
             Raptor client2 = new Raptor(network)) {
            network.startAll();

            server.runConfiguration("""
                    {
                      "service": "sse",
                      "role": "server",
                      "port": 50000,
                      "sendStrategy": "interactive",
                      "commandSubstitutionTimeout": 1000,
                      "tlsVersion": "none"
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Waiting for client to connect");

            String clientConfiguration = """
                    {
                      "service": "sse",
                      "role": "client",
                      "uri": "http://%s:50000/events",
                      "tlsVersion": "none"
                    }
                    """;
            client1.runConfiguration(String.format(clientConfiguration, server.getRaptorHostname()));
            client2.runConfiguration(String.format(clientConfiguration, server.getRaptorHostname()));
            server.expectNumberOfOutputLineContains(2, "connected");
            client1.expectNumberOfOutputLineContains(1, "connected to server at");
            client2.expectNumberOfOutputLineContains(1, "connected to server at");

            String textMessage = "Hello, subscribers!";
            sendEvent(server, textMessage, "", "");
            client1.expectNumberOfOutputLineContains(1, "received", textMessage);
            client2.expectNumberOfOutputLineContains(1, "received", textMessage);
        }
    }

    private static void sendEvent(Raptor server, String data, String id, String event) {
        server.writeLineToStdIn(data);
        server.writeLineToStdIn(id);
        server.writeLineToStdIn(event);
    }
}
