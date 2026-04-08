package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import com.github.trosenkrantz.raptor.UniqueIpPort;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class UdpBroadcastIntegrationTest extends RaptorIntegrationTest {
    @Test
    public void sendAndReceiveBroadcast() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network);
             UniqueIpPort port = UniqueIpPort.claim()) {
            network.startAll();

            // Arrange
            receiver.runConfiguration(String.format("""
                    {
                      "service" : "udp",
                      "mode" : "broadcast",
                      "role" : "receive",
                      "localPort" : %s
                    }
                    """, port.get()));
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive"); // Wait until ready to receive

            // Act
            sender.runConfiguration(String.format("""
                    {
                      "service" : "udp",
                      "mode" : "broadcast",
                      "role" : "send",
                      "remotePort" : %s,
                      "payload" : "Hello, World!",
                      "commandSubstitutionTimeout": 1000
                    }
                    """,  port.get()));

            // Assert
            sender.expectAnyOutputLineContains("sent", "text", "Hello, World!", sender.getRaptorIpAddress(), port.getString());
            receiver.expectAnyOutputLineContains("received", "text", "Hello, World!", sender.getRaptorIpAddress(), port.getString());
        }
    }
}
