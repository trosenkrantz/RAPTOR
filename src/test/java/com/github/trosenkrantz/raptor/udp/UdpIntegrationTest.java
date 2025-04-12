package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class UdpIntegrationTest {
    @Test
    public void sendAndReceiveBroadcast() {
        // Arrange
        try (Network network = Network.newNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--mode=broadcast --local-port=50000 --role=receive --service=udp")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1)); // Wait until ready to receive
             Raptor sender = new Raptor(network).withCommand("--mode=broadcast --role=send \"--payload=Hello, World\\!\" --service=udp --remote-port=50000")
                     .dependsOn(receiver)) { // Wait until receiver is ready to receive

            // Act
            receiver.start();
            sender.start();

            // Assert
            sender.expectOutputLineContains("sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "5000");
            receiver.expectOutputLineContains("received", "text", "Hello, World!", sender.getRaptorIpAddress(), "5000");
        }
    }

    @Test
    public void sendAndReceiveMulticast() {
        // Arrange
        try (Network network = Network.newNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--mode=multicast --local-port=50000 --role=receive --service=udp --remote-address=224.0.2.0")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1)); // Wait until ready to receive
             Raptor sender = new Raptor(network).withCommand("--mode=multicast --role=send \"--payload=Hello, World\\!\" --service=udp --remote-address=224.0.2.0 --remote-port=50000")
                     .dependsOn(receiver)) { // Wait until receiver is ready to receive

            // Act
            receiver.start();
            sender.start();

            // Assert
            sender.expectOutputLineContains("sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            receiver.expectOutputLineContains("received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
        }
    }

    @Test
    public void sendAndReceiveUnicastViaHostName() {
        // Arrange
        try (Network network = Network.newNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--mode=unicast --local-port=50000 --role=receive --service=udp")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1))) { // Wait until ready to receive

            receiver.start(); // Receiver must be started before we know its hostname

            try (Raptor sender = new Raptor(network)
                    .withCommand("--mode=unicast --role=send \"--payload=Hello, World\\!\" --service=udp --remote-address=" + receiver.getRaptorHostname() + " --remote-port=50000")
                    .dependsOn(receiver)) {

                // Act
                sender.start();

                // Assert
                sender.expectOutputLineContains("sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
                receiver.expectOutputLineContains("received", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
            }
        }
    }

    @Test
    public void sendAndReceiveUnicastViaIpAddress() {
        // Arrange
        try (Network network = Network.newNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--mode=unicast --local-port=50000 --role=receive --service=udp")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1))) { // Wait until ready to receive

            receiver.start(); // Receiver must be started before we know its IP address

            try (Raptor sender = new Raptor(network)
                    .withCommand("--mode=unicast --role=send \"--payload=Hello, World\\!\" --service=udp --remote-address=" + receiver.getRaptorIpAddress() + " --remote-port=50000")
                    .dependsOn(receiver)) {

                // Act
                sender.start();

                // Assert
                sender.expectOutputLineContains("sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
                receiver.expectOutputLineContains("received", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
            }
        }
    }
}
