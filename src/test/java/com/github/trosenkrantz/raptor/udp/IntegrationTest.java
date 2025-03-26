package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.concurrent.TimeoutException;

public class IntegrationTest {
    @Test
    public void sendAndReceiveMulticast() throws TimeoutException {
        try (Network network = Network.newNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--mode=multicast", "--local-port=50000", "--role=receive", "--service=udp", "--remote-address=224.0.2.0")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive data.*", 1)); // Wait until ready to receive
             Raptor sender = new Raptor(network).withCommand("--mode=multicast", "--role=send", "\"--payload=Hello, World\\!\"", "--service=udp", "--remote-address=224.0.2.0", "--remote-port=50000")
                     .dependsOn(receiver)) { // Wait until receiver is ready to receive

            receiver.start();
            sender.start();

            receiver.assertOutput("Hello, World!");
        }
    }
}
