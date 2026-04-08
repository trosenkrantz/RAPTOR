package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import com.github.trosenkrantz.raptor.UniqueIpPort;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class UdpMulticastGatewayIntegrationTest extends RaptorIntegrationTest {
    /**
     * Sender -> gateway -> receiver.
     */
    @Test
    public void OneWayGateway() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor gateway = new Raptor(network);
             Raptor sender = new Raptor(network);
             UniqueIpPort port = UniqueIpPort.claim()) {
            network.startAll();

            // Arrange
            receiver.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "receive",
                      "remoteAddress": "224.0.2.1",
                      "localPort": %s
                    }
                    """, port.get()));
            gateway.runConfiguration(String.format("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.0",
                        "port": %s
                      },
                      "b": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.1",
                        "port": %s
                      }
                    }
                    """, port.get(), port.get()));
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive");
            gateway.expectNumberOfOutputLineContains(2, "Waiting to receive"); // One for each endpoint

            // Act
            sender.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "send",
                      "remoteAddress": "224.0.2.0",
                      "remotePort": %s,
                      "payload": "Hello, World!",
                      "commandSubstitutionTimeout": 1000
                    }
                    """, port.get()));

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.0", port.getString());
            gateway.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", port.getString());
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.1", port.getString());
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", gateway.getRaptorIpAddress(), "224.0.2.1", port.getString());
        }
    }

    /**
     * Gateway is bridging endpoint A and B.
     * SenderA -> endpoint A -> gateway -> endpoint B -> receiverB.
     * SenderB -> endpoint B -> gateway -> endpoint A -> receiverA.
     */
    @Test
    public void TwoWayGateway() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiverA = new Raptor(network);
             Raptor receiverB = new Raptor(network);
             Raptor gateway = new Raptor(network);
             Raptor senderA = new Raptor(network);
             Raptor senderB = new Raptor(network);
             UniqueIpPort port = UniqueIpPort.claim()) {
            network.startAll();

            // Arrange
            receiverA.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "receive",
                      "remoteAddress": "224.0.2.1",
                      "localPort": %s
                    }
                    """, port.get()));
            receiverB.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "receive",
                      "remoteAddress": "224.0.2.2",
                      "localPort": %s
                    }
                    """, port.get()));
            gateway.runConfiguration(String.format("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.1",
                        "port": %s
                      },
                      "b": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.2",
                        "port": %s
                      }
                    }
                    """, port.get(), port.get()));
            receiverA.expectNumberOfOutputLineContains(1, "Waiting to receive");
            receiverB.expectNumberOfOutputLineContains(1, "Waiting to receive");
            gateway.expectNumberOfOutputLineContains(2, "Waiting to receive"); // One for each endpoint

            // Act
            senderA.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "send",
                      "remoteAddress": "224.0.2.1",
                      "remotePort": %s,
                      "payload": "Hello, World! 1",
                      "commandSubstitutionTimeout": 1000
                    }
                    """, port.get()));
            senderB.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "send",
                      "remoteAddress": "224.0.2.2",
                      "remotePort": %s,
                      "payload": "Hello, World! 2",
                      "commandSubstitutionTimeout": 1000
                    }
                    """, port.get()));


            // Assert

            // Sender 1 -> gateway -> Receiver 2
            senderA.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 1", "224.0.2.1", port.getString());
            gateway.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 1", senderA.getRaptorIpAddress(), "224.0.2.1", port.getString());
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 1", "224.0.2.2", port.getString());
            receiverB.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 1", gateway.getRaptorIpAddress(), "224.0.2.2", port.getString());

            // Sender 2 -> gateway -> Receiver 1
            senderB.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 2", "224.0.2.2", port.getString());
            gateway.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 2", senderB.getRaptorIpAddress(), "224.0.2.2", port.getString());
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 2", "224.0.2.1", port.getString());
            receiverA.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 2", gateway.getRaptorIpAddress(), "224.0.2.1", port.getString());
        }
    }

    /**
     * Sender -> gateway1 -> gateway2 -> receiver.
     */
    @Test
    public void twoGatewaysInSequence() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor gateway1 = new Raptor(network);
             Raptor gateway2 = new Raptor(network);
             Raptor sender = new Raptor(network);
             UniqueIpPort port = UniqueIpPort.claim()) {
            network.startAll();

            // Arrange
            receiver.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "receive",
                      "remoteAddress": "224.0.2.2",
                      "localPort": %s
                    }
                    """, port.get()));
            gateway1.runConfiguration(String.format("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.0",
                        "port": %s
                      },
                      "b": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.1",
                        "port": %s
                      }
                    }
                    """, port.get(), port.get()));
            gateway2.runConfiguration(String.format("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.1",
                        "port": %s
                      },
                      "b": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.2",
                        "port": %s
                      }
                    }
                    """, port.get(), port.get()));
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive");
            gateway1.expectNumberOfOutputLineContains(2, "Waiting to receive"); // One for each endpoint
            gateway2.expectNumberOfOutputLineContains(2, "Waiting to receive"); // One for each endpoint

            // Act
            sender.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "send",
                      "remoteAddress": "224.0.2.0",
                      "remotePort": %s,
                      "payload": "Hello, World!",
                      "commandSubstitutionTimeout": 1000
                    }
                    """, port.get()));

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.0", port.getString());
            gateway1.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", port.getString());
            gateway1.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.1", port.getString());
            gateway2.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", gateway1.getRaptorIpAddress(), "224.0.2.1", port.getString());
            gateway2.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.2", port.getString());
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", gateway2.getRaptorIpAddress(), "224.0.2.2", port.getString());
        }
    }
}
