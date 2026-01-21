package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

import java.io.IOException;

public class TcpIntegrationTest extends RaptorIntegrationTest {
    @Test
    public void twoClients() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor client1 = new Raptor(network);
             Raptor client2 = new Raptor(network)) {
            network.startAll();

            server.runConfiguration("""
                    {
                      "service": "tcp",
                      "role": "server",
                      "localPort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "none"
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Waiting for client to connect");

            // Client connects to server
            client1.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remoteHost": "%s",
                      "remotePort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "none"
                    }
                    """, server.getRaptorHostname()));
            server.expectNumberOfOutputLineContains(1, "connected", client1.getRaptorIpAddress(), "50000");
            client1.expectNumberOfOutputLineContains(1, "connected", server.getRaptorIpAddress(), "50000");

            // Client disconnects
            client1.stop();
            server.expectNumberOfOutputLineContains(1, "socket closed");
            server.expectNumberOfOutputLineContains(2, "waiting for client to connect", "50000");

            // Other client connects to the server again
            client2.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remoteHost": "%s",
                      "remotePort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "none"
                    }
                    """, server.getRaptorHostname()));
            server.expectNumberOfOutputLineContains(1, "connected", client2.getRaptorIpAddress(), "50000");
            client2.expectNumberOfOutputLineContains(1, "connected", server.getRaptorIpAddress(), "50000");
        }
    }

    @Test
    public void clientConnectsWithIpAddress() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor client = new Raptor(network)) {
            network.startAll();

            server.runConfiguration("""
                    {
                      "service": "tcp",
                      "role": "server",
                      "localPort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "none"
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Waiting for client to connect");

            // Client connects to server
            client.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remoteHost": "%s",
                      "remotePort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "none"
                    }
                    """, server.getRaptorIpAddress()));
            server.expectNumberOfOutputLineContains(1, "connected", client.getRaptorIpAddress(), "50000");
            client.expectNumberOfOutputLineContains(1, "connected", server.getRaptorIpAddress(), "50000");
        }
    }

    @Test
    public void interactive() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor client = new Raptor(network)) {
            network.startAll();

            server.runConfiguration("""
                    {
                      "service": "tcp",
                      "role": "server",
                      "localPort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Waiting for client to connect");

            // Client connects to server
            client.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remoteHost": "%s",
                      "remotePort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """, server.getRaptorHostname()));
            String serverAddress = server.getRaptorIpAddress();
            String clientAddress = client.getRaptorIpAddress();
            server.expectNumberOfOutputLineContains(1, "connected", serverAddress, clientAddress, "50000");
            client.expectNumberOfOutputLineContains(1, "connected", serverAddress, clientAddress, "50000");

            // Server sends a message to the client
            String textMessage = "Hello, World!";
            server.writeLineToStdIn(textMessage);
            server.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            client.expectNumberOfOutputLineContains(1, "received", "text", textMessage);

            // Client sends a message to the server
            String binaryMessage = "\\\\x00\\\\x01\\\\x02\\\\x03";
            client.writeLineToStdIn(binaryMessage);
            client.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            server.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
        }
    }
}
