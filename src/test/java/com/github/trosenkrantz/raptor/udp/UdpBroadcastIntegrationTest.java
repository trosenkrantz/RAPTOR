package com.github.trosenkrantz.raptor.udp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class UdpBroadcastIntegrationTest extends RaptorIntegrationTest {
    @Test
    public void sendAndReceiveBroadcast() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
                    {
                      "service" : "udp",
                      "mode" : "broadcast",
                      "role" : "receive",
                      "localPort" : 50000
                    }
                    """);
            receiver.expectNumberOfOutputLineContains(1, "Waiting to receive"); // Wait until ready to receive

            // Act
            sender.runConfiguration("""
                    {
                      "service" : "udp",
                      "mode" : "broadcast",
                      "role" : "send",
                      "remotePort" : 50000,
                      "payload" : "Hello, World!"
                    }
                    """);

            // Assert
            sender.expectAnyOutputLineContains("sent", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
            receiver.expectAnyOutputLineContains("received", "text", "Hello, World!", sender.getRaptorIpAddress(), "50000");
        }
    }
}
