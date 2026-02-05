package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class UdpIntegrationTest extends RaptorIntegrationTest {
    @Test
    public void sendAndReceiveBroadcast() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
                    {
                      "service" : "udp",
                      "mode" : "broadcast",
                      "role" : "receive",
                      "localPort" : 50000
                    }
                    """);
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive"); // Wait until ready to receive

            // Act
            sender.runConfiguration("""
                    {
                      "service" : "udp",
                      "mode" : "broadcast",
                      "role" : "send",
                      "remotePort" : 50000,
                      "payload" : "Hello, World!"
                    }
                    """);

            // Assert
            sender.expectAnyOutputLineContains("sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
            receiver.expectAnyOutputLineContains("received", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
        }
    }

    @Test
    public void sendAndReceiveMulticast() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
                    {
                      "service" : "udp",
                      "mode" : "multicast",
                      "role" : "receive",
                      "remoteAddress" : "224.0.2.0",
                      "localPort" : 50000
                    }
                    """);
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive"); // Wait until ready to receive

            // Act
            sender.runConfiguration("""
                    {
                      "service" : "udp",
                      "mode" : "multicast",
                      "role" : "send",
                      "remoteAddress" : "224.0.2.0",
                      "remotePort" : 50000,
                      "payload" : "Hello, World!"
                    }
                    """);

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
        }
    }

    @Test
    public void sendAndReceiveUnicastViaHostName() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
                    {
                      "service": "udp",
                      "mode": "unicast",
                      "role": "receive",
                      "localPort": 50000
                    }
                    """);
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive");

            // Act
            sender.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "unicast",
                      "role": "send",
                      "remoteAddress": "%s",
                      "remotePort": 50000,
                      "payload": "Hello, World!"
                    }
                    """, receiver.getRaptorHostname()));

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
        }
    }

    @Test
    public void sendAndReceiveUnicastViaIpAddress() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
                    {
                      "service": "udp",
                      "mode": "unicast",
                      "role": "receive",
                      "localPort": 50000
                    }
                    """);
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive");

            // Act
            sender.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "unicast",
                      "role": "send",
                      "remoteAddress": "%s",
                      "remotePort": 50000,
                      "payload": "Hello, World!"
                    }
                    """, receiver.getRaptorIpAddress()));

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
        }
    }
}
