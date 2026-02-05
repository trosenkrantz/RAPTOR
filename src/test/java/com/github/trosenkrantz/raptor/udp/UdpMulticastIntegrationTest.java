package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Docker only supports IPv4 default, so sticking this that.
 */
public class UdpMulticastIntegrationTest extends RaptorIntegrationTest {
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
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.0", "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", "50000");
        }
    }
}
