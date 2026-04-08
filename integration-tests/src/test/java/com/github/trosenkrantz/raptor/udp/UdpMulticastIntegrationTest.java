package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import com.github.trosenkrantz.raptor.UniqueIpPort;
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
             Raptor sender = new Raptor(network);
             UniqueIpPort port = UniqueIpPort.claim()) {
            network.startAll();

            // Arrange
            receiver.runConfiguration(String.format("""
                    {
                      "service" : "udp",
                      "mode" : "multicast",
                      "role" : "receive",
                      "remoteAddress" : "224.0.2.0",
                      "localPort" : %s
                    }
                    """, port.get()));
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive"); // Wait until ready to receive

            // Act
            sender.runConfiguration(String.format("""
                    {
                      "service" : "udp",
                      "mode" : "multicast",
                      "role" : "send",
                      "remoteAddress" : "224.0.2.0",
                      "remotePort" : %s,
                      "payload" : "Hello, World!",
                      "commandSubstitutionTimeout": 1000
                    }
                    """, port.get()));

            // Assert
            sender.expectNumberOfOutputLineContains(1, "sent", "text", "Hello, World!", "224.0.2.0", port.getString());
            receiver.expectNumberOfOutputLineContains(1, "received", "text", "Hello, World!", sender.getRaptorIpAddress(), "224.0.2.0", port.getString());
        }
    }
}
