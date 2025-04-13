package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class UdpIntegrationTest {
    @Test
    public void sendAndReceiveBroadcast() {
        // Arrange
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--service=udp --mode=broadcast --role=receive --local-port=50000")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1)); // Wait until ready to receive
             Raptor sender = new Raptor(network).withCommand("--service=udp --mode=broadcast --role=send --remote-port=50000 \"--payload=Hello, World\\!\"")
                     .dependsOn(receiver)) { // Wait until receiver is ready to receive

            // Act
            receiver.start();
            sender.start();

            // Assert
            sender.expectAnyOutputLineContains("sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "5000");
            receiver.expectAnyOutputLineContains("received", "text", "Hello, World!", sender.getRaptorIpAddress(), "5000");
        }
    }

    @Test
    public void sendAndReceiveMulticast() {
        // Arrange
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--service=udp --mode=multicast --role=receive --remote-address=224.0.2.0 --local-port=50000")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1)); // Wait until ready to receive
             Raptor sender = new Raptor(network).withCommand("--service=udp --mode=multicast --role=send --remote-address=224.0.2.0 --remote-port=50000 \"--payload=Hello, World\\!\"")
                     .dependsOn(receiver)) { // Wait until receiver is ready to receive

            // Act
            receiver.start();
            sender.start();

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
        }
    }

    @Test
    public void sendAndReceiveUnicastViaHostName() {
        // Arrange
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--service=udp --mode=unicast --role=receive --local-port=50000")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1))) { // Wait until ready to receive

            receiver.start(); // Receiver must be started before we know its hostname

            try (Raptor sender = new Raptor(network)
                    .withCommand("--service=udp --mode=unicast --role=send --remote-address=" + receiver.getRaptorHostname() + " --remote-port=50000 \"--payload=Hello, World\\!\"")
                    .dependsOn(receiver)) {

                // Act
                sender.start();

                // Assert
                sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
                receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
            }
        }
    }

    @Test
    public void sendAndReceiveUnicastViaIpAddress() {
        // Arrange
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--mode=unicast --local-port=50000 --role=receive --service=udp")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1))) { // Wait until ready to receive

            receiver.start(); // Receiver must be started before we know its IP address

            try (Raptor sender = new Raptor(network)
                    .withCommand("--service=udp --mode=unicast --role=send --remote-address=" + receiver.getRaptorIpAddress() + " --remote-port=50000 \"--payload=Hello, World\\!\"")
                    .dependsOn(receiver)) {

                // Act
                sender.start();

                // Assert
                sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
                receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
            }
        }
    }
}
