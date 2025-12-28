package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TcpGatewayIntegrationTest extends RaptorIntegrationTest {
    /**
     * System1 (client) <-> Endpoint A (server) <-> gateway <-> Endpoint B (server) <-> System2 (client).
     */
    @Test
    public void serverEndpoints() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor gateway = new Raptor(network);
             Raptor system1 = new Raptor(network);
             Raptor system2 = new Raptor(network)) {
            network.startAll();

            // Start gateway with two TCO server sockets
            gateway.runConfiguration("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "tcp",
                        "role": "server",
                        "local-port": 50000,
                        "tlsVersion": "none"
                      },
                      "b": {
                        "endpoint": "tcp",
                        "role": "server",
                        "local-port": 50001,
                        "tlsVersion": "none"
                      }
                    }
                    """);

            // Wait for the gateway to start listening on both ports
            gateway.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");
            gateway.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50001");

            // Start systems which will connect to the gateway
            system1.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remote-host": "%s",
                      "remote-port": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """, gateway.getRaptorHostname()));
            system2.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remote-host": "%s",
                      "remote-port": 50001,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """, gateway.getRaptorHostname()));
            gateway.expectNumberOfOutputLineContains(1, "connected", system1.getRaptorIpAddress(), "50000");
            gateway.expectNumberOfOutputLineContains(1, "connected", system2.getRaptorIpAddress(), "50001");
            system1.expectNumberOfOutputLineContains(1, "connected", gateway.getRaptorIpAddress(), "50000");
            system2.expectNumberOfOutputLineContains(1, "connected", gateway.getRaptorIpAddress(), "50001");

            // System1 sends a text message
            String textMessage = "Hello, World!";
            system1.writeLineToStdIn(textMessage);
            system1.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            gateway.expectNumberOfOutputLineContains(1, "received", "text", textMessage);
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            system2.expectNumberOfOutputLineContains(1, "received", "text", textMessage);

            // System2 sends a binary message
            String binaryMessage = "\\\\x00\\\\x01\\\\x02\\\\x03";
            system2.writeLineToStdIn(binaryMessage);
            system2.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            gateway.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
            gateway.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            system1.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
        }
    }

    /**
     * System1 (server) <-> Endpoint A (client) <-> gateway <-> Endpoint B (client) <-> System2 (server).
     */
    @Test
    public void clientEndpoints() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor gateway = new Raptor(network);
             Raptor system1 = new Raptor(network);
             Raptor system2 = new Raptor(network)) {
            network.startAll();

            // Start system1 and system2
            system1.runConfiguration("""
                    {
                      "service": "tcp",
                      "role": "server",
                      "local-port": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """);
            system2.runConfiguration("""
                    {
                      "service": "tcp",
                      "role": "server",
                      "local-port": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """);

            // Wait for both systems to start listening
            system1.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");
            system2.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

            // Start gateway with two TCP client sockets which will connect to the systems
            gateway.runConfiguration(String.format("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "tcp",
                        "role": "client",
                        "remote-host": "%s",
                        "remote-port": 50000,
                        "tlsVersion": "none"
                      },
                      "b": {
                        "endpoint": "tcp",
                        "role": "client",
                        "remote-host": "%s",
                        "remote-port": 50000,
                        "tlsVersion": "none"
                      }
                    }
                    """, system1.getRaptorHostname(), system2.getRaptorHostname()));
            gateway.expectNumberOfOutputLineContains(1, "connected", system1.getRaptorIpAddress(), "50000");
            gateway.expectNumberOfOutputLineContains(1, "connected", system2.getRaptorIpAddress(), "50000");
            system1.expectNumberOfOutputLineContains(1, "connected", gateway.getRaptorIpAddress(), "50000");
            system2.expectNumberOfOutputLineContains(1, "connected", gateway.getRaptorIpAddress(), "50000");

            // System1 sends a text message
            String textMessage = "Hello, World!";
            system1.writeLineToStdIn(textMessage);
            system1.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            gateway.expectNumberOfOutputLineContains(1, "received", "text", textMessage);
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            system2.expectNumberOfOutputLineContains(1, "received", "text", textMessage);

            // System2 sends a binary message
            String binaryMessage = "\\\\x00\\\\x01\\\\x02\\\\x03";
            system2.writeLineToStdIn(binaryMessage);
            system2.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            gateway.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
            gateway.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            system1.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
        }
    }

    /**
     * system1 (server) <-> gateway1 endpoint A (client) <-> gateway1 endpoint B (server) <-> gateway2 endpoint A (client) <-> gateway2 endpoint B (server) <-> system2 (client).
     */
    @Test
    public void twoGatewaysInSequence() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor gateway1 = new Raptor(network);
             Raptor gateway2 = new Raptor(network);
             Raptor system1 = new Raptor(network);
             Raptor system2 = new Raptor(network)) {
            network.startAll();

            // Start system1
            system1.runConfiguration("""
                    {
                      "service": "tcp",
                      "role": "server",
                      "local-port": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """);
            system1.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

            // Start gateway1
            gateway1.runConfiguration(String.format("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "tcp",
                        "role": "client",
                        "remote-host": "%s",
                        "remote-port": 50000,
                        "tlsVersion": "none"
                      },
                      "b": {
                        "endpoint": "tcp",
                        "role": "server",
                        "local-port": 50000,
                        "tlsVersion": "none"
                      }
                    }
                    """, system1.getRaptorHostname()));
            gateway1.expectNumberOfOutputLineContains(1, "connected", system1.getRaptorIpAddress(), "50000");
            gateway1.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

            // Start gateway1
            gateway2.runConfiguration(String.format("""
                    {
                      "service": "gateway",
                      "a": {
                        "endpoint": "tcp",
                        "role": "client",
                        "remote-host": "%s",
                        "remote-port": 50000,
                        "tlsVersion": "none"
                      },
                      "b": {
                        "endpoint": "tcp",
                        "role": "server",
                        "local-port": 50000,
                        "tlsVersion": "none"
                      }
                    }
                    """, gateway1.getRaptorHostname()));
            gateway2.expectNumberOfOutputLineContains(1, "connected", gateway1.getRaptorIpAddress(), "50000");
            gateway2.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

            // Start system2
            system2.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remote-host": "%s",
                      "remote-port": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """, gateway2.getRaptorHostname()));
            system2.expectNumberOfOutputLineContains(1, "connected", gateway2.getRaptorIpAddress(), "50000");

            // System1 sends a text message
            String textMessage = "Hello, World!";
            system1.writeLineToStdIn(textMessage);
            system1.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            gateway1.expectNumberOfOutputLineContains(1, "received", "text", textMessage);
            gateway1.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            gateway2.expectNumberOfOutputLineContains(1, "received", "text", textMessage);
            gateway2.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            system2.expectNumberOfOutputLineContains(1, "received", "text", textMessage);

            // System2 sends a binary message
            String binaryMessage = "\\\\x00\\\\x01\\\\x02\\\\x03";
            system2.writeLineToStdIn(binaryMessage);
            system2.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            gateway1.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
            gateway1.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            gateway2.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
            gateway2.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            system1.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
        }
    }
}
