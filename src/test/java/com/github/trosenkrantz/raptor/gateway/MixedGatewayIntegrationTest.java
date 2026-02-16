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
            system1.runConfiguration("""
                    {
                      "service": "tcp",
                      "role": "server",
                      "localPort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive",
                      "commandSubstitutionTimeout": 1000
                    }
                    """);
            system1.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

            // Start gateway
            gateway.runConfiguration(String.format("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "tcp",
                        "role": "client",
                        "remoteHost": "%s",
                        "remotePort": 50000,
                        "tlsVersion": "none",
                        "commandSubstitutionTimeout": 1000
                      },
                      "b": {
                        "endpoint": "udp",
                        "mode": "multicast",
                        "remoteAddress": "224.0.2.0",
                        "port": 50000
                      }
                    }
                    """, system1.getRaptorHostname())); // TODO UDP does not require timeout, it should probably do that
            gateway.expectNumberOfOutputLineContains(1, "connected", system1.getRaptorIpAddress(), "50000");

            // Start system2 receiver
            system2Receiver.runConfiguration("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "receive",
                      "remoteAddress": "224.0.2.0",
                      "localPort": 50000
                    }
                    """);
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
            system2Sender.runConfiguration(String.format("""
                    {
                      "service": "udp",
                      "mode": "multicast",
                      "role": "send",
                      "remoteAddress": "224.0.2.0",
                      "remotePort": 50000,
                      "payload": "%s",
                      "commandSubstitutionTimeout": 1000
                    }
                    """, binaryMessage));
            system2Sender.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            gateway.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
            gateway.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            system1.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
        }
    }
}
