package com.github.trosenkrantz.raptor.gateway.network.impairment;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class CorruptionIntegrationTest {
    /**
     * Sender -> gateway -> receiver.
     */
    @Test
    public void flipAllBits() throws IOException {
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
                      },
                      "aToB": {
                        "corruption": 1.0
                      }
                    }
                    """); // Flip all bits to get a deterministic test
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive");
            gateway.expectNumberOfOutputLineContains(2, "Waiting to receive"); // One for each endpoint

            // Act
            String originalBinaryMessage = "\\\\x00\\\\x01\\\\x02\\\\x03";
            sender.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "send",
                      "remoteAddress": "224.0.2.0",
                      "remotePort": 50000,
                      "payload": "%s"
                    }
                    """, originalBinaryMessage));

            // Assert
            String expectedReceivedBinaryMessage = "\\\\xff\\\\xfe\\\\xfd\\\\xfc";
            sender.expectNumberOfOutputLineContains(1, "sent", "bytes", originalBinaryMessage, "224.0.2.0", "50000");
            gateway.expectNumberOfOutputLineContains(1, "received", "bytes", originalBinaryMessage, sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway.expectNumberOfOutputLineContains(1, "sent", "bytes", expectedReceivedBinaryMessage, "224.0.2.1", "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "bytes", expectedReceivedBinaryMessage, gateway.getRaptorIpAddress(), "224.0.2.1", "50000");
        }
    }
}