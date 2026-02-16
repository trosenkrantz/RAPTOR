package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class UdpUnicastIntegrationTest extends RaptorIntegrationTest {
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
                      "payload": "Hello, World!",
                      "commandSubstitutionTimeout": 1000
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
                      "payload": "Hello, World!",
                      "commandSubstitutionTimeout": 1000
                    }
                    """, receiver.getRaptorIpAddress()));

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
        }
    }
}
