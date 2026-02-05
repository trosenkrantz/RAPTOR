package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
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
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "receive",
                      "remoteAddress": "224.0.2.1",
                      "localPort": 50000
                    }
                    """);
            gateway.runConfiguration("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.0",
                        "port": 50000
                      },
                      "b": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.1",
                        "port": 50000
                      }
                    }
                    """);
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive");
            gateway.expectNumberOfOutputLineContains(2, "Waiting to receive"); // One for each endpoint

            // Act
            sender.runConfiguration("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "send",
                      "remoteAddress": "224.0.2.0",
                      "remotePort": 50000,
                      "payload": "Hello, World!"
                    }
                    """);

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.0", "50000");
            gateway.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.1", "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", gateway.getRaptorIpAddress(), "224.0.2.1", "50000");
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
             Raptor senderB = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiverA.runConfiguration("""
            {
              "service": "udp",
              "mode": "multicast",
              "role": "receive",
              "remoteAddress": "224.0.2.1",
              "localPort": 50001
            }
            """);
            receiverB.runConfiguration("""
            {
              "service": "udp",
              "mode": "multicast",
              "role": "receive",
              "remoteAddress": "224.0.2.2",
              "localPort": 50002
            }
            """);
            gateway.runConfiguration("""
            {
              "service": "gateway",
              "a": {
                "endpoint": "udp",
                "mode": "multicast",
                "remoteAddress": "224.0.2.1",
                "port": 50001
              },
              "b": {
                "endpoint": "udp",
                "mode": "multicast",
                "remoteAddress": "224.0.2.2",
                "port": 50002
              }
            }
            """);
            receiverA.expectNumberOfOutputLineContains(1, "Waiting to receive");
            receiverB.expectNumberOfOutputLineContains(1, "Waiting to receive");
            gateway.expectNumberOfOutputLineContains(2, "Waiting to receive"); // One for each endpoint

            // Act
            senderA.runConfiguration("""
            {
              "service": "udp",
              "mode": "multicast",
              "role": "send",
              "remoteAddress": "224.0.2.1",
              "remotePort": 50001,
              "payload": "Hello, World! 1"
            }
            """);
            senderB.runConfiguration("""
            {
              "service": "udp",
              "mode": "multicast",
              "role": "send",
              "remoteAddress": "224.0.2.2",
              "remotePort": 50002,
              "payload": "Hello, World! 2"
            }
            """);


            // Assert

            // Sender 1 -> gateway -> Receiver 2
            senderA.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 1", "224.0.2.1", "50001");
            gateway.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 1", senderA.getRaptorIpAddress(), "224.0.2.1", "50001");
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 1", "224.0.2.2", "50002");
            receiverB.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 1", gateway.getRaptorIpAddress(), "224.0.2.2", "50002");

            // Sender 2 -> gateway -> Receiver 1
            senderB.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 2", "224.0.2.2", "50002");
            gateway.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 2", senderB.getRaptorIpAddress(), "224.0.2.2", "50002");
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 2", "224.0.2.1", "50001");
            receiverA.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 2", gateway.getRaptorIpAddress(), "224.0.2.1", "50001");
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
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
            {
              "service": "udp",
              "mode": "multicast",
              "role": "receive",
              "remoteAddress": "224.0.2.2",
              "localPort": 50000
            }
            """);
            gateway1.runConfiguration("""
            {
              "service": "gateway",
              "a": {
                "endpoint": "udp",
                "mode": "multicast",
                "remoteAddress": "224.0.2.0",
                "port": 50000
              },
              "b": {
                "endpoint": "udp",
                "mode": "multicast",
                "remoteAddress": "224.0.2.1",
                "port": 50000
              }
            }
            """);
            gateway2.runConfiguration("""
            {
              "service": "gateway",
              "a": {
                "endpoint": "udp",
                "mode": "multicast",
                "remoteAddress": "224.0.2.1",
                "port": 50000
              },
              "b": {
                "endpoint": "udp",
                "mode": "multicast",
                "remoteAddress": "224.0.2.2",
                "port": 50000
              }
            }
            """);
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive");
            gateway1.expectNumberOfOutputLineContains(2, "Waiting to receive"); // One for each endpoint
            gateway2.expectNumberOfOutputLineContains(2, "Waiting to receive"); // One for each endpoint

            // Act
            sender.runConfiguration("""
            {
              "service": "udp",
              "mode": "multicast",
              "role": "send",
              "remoteAddress": "224.0.2.0",
              "remotePort": 50000,
              "payload": "Hello, World!"
            }
            """);

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.0", "50000");
            gateway1.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway1.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.1", "50000");
            gateway2.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", gateway1.getRaptorIpAddress(), "224.0.2.1", "50000");
            gateway2.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.2", "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", gateway2.getRaptorIpAddress(), "224.0.2.2", "50000");
        }
    }
}
