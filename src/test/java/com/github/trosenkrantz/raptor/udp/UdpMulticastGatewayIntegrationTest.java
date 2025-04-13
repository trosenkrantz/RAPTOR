package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class UdpMulticastGatewayIntegrationTest {
    /**
     * Sender -> gateway -> receiver.
     */
    @Test
    public void OneWayGateway() {
        // Arrange
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--service=udp --mode=multicast --role=receive --remote-address=224.0.2.1 --local-port=50000")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1)); // Wait until ready to receive
             Raptor gateway = new Raptor(network)
                     .withCommand("--service=gateway --a-endpoint=udp --a-mode=multicast --a-remote-address=224.0.2.0 --a-port=50000 --b-endpoint=udp --b-mode=multicast --b-remote-address=224.0.2.1 --b-port=50000")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 2)); // Wait until both endpoints ready to receive
             Raptor sender = new Raptor(network).withCommand("--service=udp --mode=multicast --role=send --remote-address=224.0.2.0 --remote-port=50000 \"--payload=Hello, World\\!\"")
                     .dependsOn(receiver, gateway)) {

            // Act
            receiver.start();
            gateway.start();
            sender.start();

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", gateway.getRaptorIpAddress(), "224.0.2.1", "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", gateway.getRaptorIpAddress(), "224.0.2.1", "50000");
        }
    }

    /**
     * Sender1 -> gateway -> receiver2.
     * Sender2 -> gateway -> receiver1.
     */
    @Test
    public void TwoWayGateway() {
        // Arrange
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver1 = new Raptor(network)
                     .withCommand("--service=udp --mode=multicast --role=receive --remote-address=224.0.2.1 --local-port=50001")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1)); // Wait until ready to receive
             Raptor receiver2 = new Raptor(network)
                     .withCommand("--service=udp --mode=multicast --role=receive --remote-address=224.0.2.2 --local-port=50002")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1)); // Wait until ready to receive
             Raptor gateway = new Raptor(network)
                     .withCommand("--service=gateway --a-endpoint=udp --a-mode=multicast --a-remote-address=224.0.2.1 --a-port=50001 --b-endpoint=udp --b-mode=multicast --b-remote-address=224.0.2.2 --b-port=50002")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 2)); // Wait until both endpoints ready to receive
             Raptor sender1 = new Raptor(network).withCommand("--service=udp --mode=multicast --role=send --remote-address=224.0.2.1 --remote-port=50001 \"--payload=Hello, World\\! 1\"")
                     .dependsOn(receiver2, gateway);
             Raptor sender2 = new Raptor(network).withCommand("--service=udp --mode=multicast --role=send --remote-address=224.0.2.2 --remote-port=50002 \"--payload=Hello, World\\! 2\"")
                .dependsOn(receiver1, gateway)) {

            // Act
            receiver1.start();
            receiver2.start();
            gateway.start();
            sender1.start();
            sender2.start();


            // Assert

            // Sender 1 -> gateway -> Receiver 2
            sender1.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 1", sender1.getRaptorIpAddress(), "224.0.2.1", "50001");
            gateway.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 1", sender1.getRaptorIpAddress(), "224.0.2.1", "50001");
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 1", gateway.getRaptorIpAddress(), "224.0.2.2", "50002");
            receiver2.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 1", gateway.getRaptorIpAddress(), "224.0.2.2", "50002");

            // Sender 2 -> gateway -> Receiver 1
            sender2.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 2", sender2.getRaptorIpAddress(), "224.0.2.2", "50002");
            gateway.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 2", sender2.getRaptorIpAddress(), "224.0.2.2", "50002");
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World! 2", gateway.getRaptorIpAddress(), "224.0.2.1", "50001");
            receiver1.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World! 2", gateway.getRaptorIpAddress(), "224.0.2.1", "50001");
        }
    }

    /**
     * Sender -> gateway1 -> gateway2 -> receiver.
     */
    @Test
    public void twoGatewaysInSequence() {
        // Arrange
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network)
                     .withCommand("--service=udp --mode=multicast --role=receive --remote-address=224.0.2.2 --local-port=50000")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 1)); // Wait until ready to receive
             Raptor gateway1 = new Raptor(network)
                     .withCommand("--service=gateway --a-endpoint=udp --a-mode=multicast --a-remote-address=224.0.2.0 --a-port=50000 --b-endpoint=udp --b-mode=multicast --b-remote-address=224.0.2.1 --b-port=50000")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 2)); // Wait until both endpoints ready to receive
             Raptor gateway2 = new Raptor(network)
                     .withCommand("--service=gateway --a-endpoint=udp --a-mode=multicast --a-remote-address=224.0.2.1 --a-port=50000 --b-endpoint=udp --b-mode=multicast --b-remote-address=224.0.2.2 --b-port=50000")
                     .waitingFor(Wait.forLogMessage(".*Waiting to receive.*", 2)); // Wait until both endpoints ready to receive
             Raptor sender = new Raptor(network).withCommand("--service=udp --mode=multicast --role=send --remote-address=224.0.2.0 --remote-port=50000 \"--payload=Hello, World\\!\"")
                     .dependsOn(receiver, gateway1, gateway2)) {

            // Act
            receiver.start();
            gateway1.start();
            gateway2.start();
            sender.start();

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway1.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway1.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", gateway1.getRaptorIpAddress(), "224.0.2.1", "50000");
            gateway2.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", gateway1.getRaptorIpAddress(), "224.0.2.1", "50000");
            gateway2.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", gateway2.getRaptorIpAddress(), "224.0.2.2", "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", gateway2.getRaptorIpAddress(), "224.0.2.2", "50000");
        }
    }
}
