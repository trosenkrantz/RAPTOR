package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;

public class UdpIntegrationTest {
    @Test
    public void sendAndReceiveBroadcast() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runRaptor("--service=udp --mode=broadcast --role=receive --local-port=50000");
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive"); // Wait until ready to receive

            // Act
            sender.runRaptor("--service=udp --mode=broadcast --role=send --remote-port=50000 \"--payload=Hello, World\\!\"");

            // Assert
            sender.expectAnyOutputLineContains("sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "5000");
            receiver.expectAnyOutputLineContains("received", "text", "Hello, World!", sender.getRaptorIpAddress(), "5000");
        }
    }

    @Test
    public void sendAndReceiveMulticast() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runRaptor("--service=udp --mode=multicast --role=receive --remote-address=224.0.2.0 --local-port=50000");
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive"); // Wait until ready to receive

            // Act
            sender.runRaptor("--service=udp --mode=multicast --role=send --remote-address=224.0.2.0 --remote-port=50000 \"--payload=Hello, World\\!\"");

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
            receiver.runRaptor("--service=udp --mode=unicast --role=receive --local-port=50000");
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive");

            // Act
            sender.runRaptor("--service=udp --mode=unicast --role=send --remote-address=" + receiver.getRaptorHostname() + " --remote-port=50000 \"--payload=Hello, World\\!\"");

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
            receiver.runRaptor("--mode=unicast --local-port=50000 --role=receive --service=udp")
                    .expectNumberOfOutputLineContains(1, "Waiting to receive");

            // Act
            sender.runRaptor("--service=udp --mode=unicast --role=send --remote-address=" + receiver.getRaptorIpAddress() + " --remote-port=50000 \"--payload=Hello, World\\!\"");

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
        }
    }
}
