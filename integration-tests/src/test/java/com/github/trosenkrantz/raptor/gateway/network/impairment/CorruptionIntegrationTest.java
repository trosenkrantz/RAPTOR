package com.github.trosenkrantz.raptor.gateway.network.impairment;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import com.github.trosenkrantz.raptor.UniqueIpPort;
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
             Raptor sender = new Raptor(network);
             UniqueIpPort port = UniqueIpPort.claim()) {
            network.startAll();

            // Arrange
            receiver.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "receive",
                      "remoteAddress": "224.0.2.1",
                      "localPort": %s
                    }
                    """, port.get()));
            gateway.runConfiguration(String.format("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.0",
                        "port": %s
                      },
                      "b": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.1",
                        "port": %s
                      },
                      "aToB": {
                        "corruption": 1.0
                      }
                    }
                    """, port.get(), port.get())); // Flip all bits to get a deterministic test
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
                      "remotePort": %s,
                      "payload": "%s",
                      "commandSubstitutionTimeout": 1000
                    }
                    """, port.get(), originalBinaryMessage));

            // Assert
            String expectedReceivedBinaryMessage = "\\\\xff\\\\xfe\\\\xfd\\\\xfc";
            sender.expectNumberOfOutputLineContains(1, "sent", "bytes", originalBinaryMessage, "224.0.2.0", port.getString());
            gateway.expectNumberOfOutputLineContains(1, "received", "bytes", originalBinaryMessage, sender.getRaptorIpAddress(), "224.0.2.0", port.getString());
            gateway.expectNumberOfOutputLineContains(1, "sent", "bytes", expectedReceivedBinaryMessage, "224.0.2.1", port.getString());
            receiver.expectNumberOfOutputLineContains(1, "received", "bytes", expectedReceivedBinaryMessage, gateway.getRaptorIpAddress(), "224.0.2.1", port.getString());
        }
    }
}