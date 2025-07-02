package com.github.trosenkrantz.raptor.gateway;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class MixedGatewayIntegrationTest {
    /**
     * system1 (TCP server) <-> Endpoint A (TCP client) <-> Endpoint B (UDP MC) <-> system2 (UDP MC).
     */
    @Test
    public void tcpAndUdpMulticast() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor gateway = new Raptor(network);
             Raptor system1 = new Raptor(network);
             Raptor system2Receiver = new Raptor(network);
             Raptor system2Sender = new Raptor(network)) {
            network.startAll();

            // Start system1
            system1.runRaptor("--service=tcp --role=server --local-port=50000 --tls-version=none --send-strategy=interactive");
            system1.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

            // Start gateway
            gateway.runRaptor("--service=gateway --a-endpoint=tcp --a-role=client --a-remote-host=" + system1.getRaptorHostname() + " --a-remote-port=50000 --a-tls-version=none --b-endpoint=udp --b-mode=multicast --b-remote-address=224.0.2.0 --b-port=50000");
            gateway.expectNumberOfOutputLineContains(1, "connected", system1.getRaptorIpAddress(), "50000");

            // Start system2 receiver
            system2Receiver.runRaptor("--service=udp --mode=multicast --role=receive --remote-address=224.0.2.0 --local-port=50000");
            system2Receiver.expectNumberOfOutputLineContains(1, "Waiting to receive");

            // System1 sends a text message
            String textMessage = "Hello, World!";
            system1.writeLineToStdIn(textMessage);
            system1.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            gateway.expectNumberOfOutputLineContains(1, "received", "text", textMessage);
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            system2Receiver.expectNumberOfOutputLineContains(1, "received", "text", textMessage, gateway.getRaptorIpAddress(), "224.0.2.0", "50000");

            // System2 sends a binary message
            String binaryMessage = "\\\\x00\\\\x01\\\\x02\\\\x03";
            system2Sender.runRaptor("--service=udp --mode=multicast --role=send --remote-address=224.0.2.0 --remote-port=50000 --payload=" + binaryMessage);
            system2Sender.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            gateway.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
            gateway.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            system1.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
        }
    }
}
