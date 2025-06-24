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
            receiver.runRaptor("--service=udp --mode=multicast --role=receive --remote-address=224.0.2.1 --local-port=50000");
            gateway.runRaptor("--service=gateway --a-endpoint=udp --a-mode=multicast --a-remote-address=224.0.2.0 --a-port=50000 --b-endpoint=udp --b-mode=multicast --b-remote-address=224.0.2.1 --b-port=50000 --a-to-b-corruption=1.0"); // Flip all bits to get a deterministic test
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive");
            gateway.expectNumberOfOutputLineContains(2, "Waiting to receive"); // One for each endpoint

            // Act
            String originalBinaryMessage = "\\\\x00\\\\x01\\\\x02\\\\x03";
            sender.runRaptor("--service=udp --mode=multicast --role=send --remote-address=224.0.2.0 --remote-port=50000 \"--payload=" + originalBinaryMessage + "\"");

            // Assert
            String expectedReceivedBinaryMessage = "\\\\xff\\\\xfe\\\\xfd\\\\xfc";
            sender.expectNumberOfOutputLineContains(1, "sent", "bytes", originalBinaryMessage, sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway.expectNumberOfOutputLineContains(1, "received", "bytes", originalBinaryMessage, sender.getRaptorIpAddress(), "224.0.2.0", "50000");
            gateway.expectNumberOfOutputLineContains(1, "sent", "bytes", expectedReceivedBinaryMessage, gateway.getRaptorIpAddress(), "224.0.2.1", "50000");
            receiver.expectNumberOfOutputLineContains(1, "received", "bytes", expectedReceivedBinaryMessage, gateway.getRaptorIpAddress(), "224.0.2.1", "50000");
        }
    }
}