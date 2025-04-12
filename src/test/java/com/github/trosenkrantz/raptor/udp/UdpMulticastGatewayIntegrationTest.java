package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.concurrent.TimeoutException;

public class UdpMulticastGatewayIntegrationTest {
    @Test
    public void sendAndReceiveMulticast() {
        // Arrange
        try (Network network = Network.newNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--mode=multicast", "--local-port=50001", "--role=receive", "--service=udp", "--remote-address=224.0.2.1")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1)); // Wait until ready to receive
             Raptor gateway = new Raptor(network)
                     .withCommand("--a-endpoint=udp --a-remote-address=224.0.2.0 --b-port=50001 --service=gateway --b-mode=multicast --b-remote-address=224.0.2.1 --a-port=50000 --b-endpoint=udp --a-mode=multicast")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 2)); // Wait until both endpoints ready to receive
             Raptor sender = new Raptor(network).withCommand("--mode=multicast", "--role=send", "\"--payload=Hello, World\\!\"", "--service=udp", "--remote-address=224.0.2.0", "--remote-port=50000")
                     .dependsOn(receiver, gateway)) {

            // Act
            receiver.start();
            sender.start();
            gateway.start();

            // Assert
            sender.expectOutputLineContains("sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway.expectOutputLineContains("received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway.expectOutputLineContains("sent", "text", "Hello, World!", gateway.getRaptorIpAddress(), "224.0.2.1", "50001");
            receiver.expectOutputLineContains("received", "text", "Hello, World!", gateway.getRaptorIpAddress(), "224.0.2.1", "50001");
        }
    }
}
