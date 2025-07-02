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
            gateway.runRaptor("--service=gateway --a-endpoint=tcp --a-role=server --a-local-port=50000 --a-tls-version=none --b-endpoint=tcp --b-role=server --b-local-port=50001 --b-tls-version=none");

            // Wait for the gateway to start listening on both ports
            gateway.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");
            gateway.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50001");

            // Start systems which will connect to the gateway
            system1.runRaptor("--service=tcp --role=client --remote-host=" + gateway.getRaptorHostname() + " --remote-port=50000 --tls-version=none --send-strategy=interactive");
            system2.runRaptor("--service=tcp --role=client --remote-host=" + gateway.getRaptorHostname() + " --remote-port=50001 --tls-version=none --send-strategy=interactive");
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
            system1.runRaptor("--service=tcp --role=server --local-port=50000 --tls-version=none --send-strategy=interactive");
            system2.runRaptor("--service=tcp --role=server --local-port=50000 --tls-version=none --send-strategy=interactive");

            // Wait for both systems to start listening
            system1.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");
            system2.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

            // Start gateway with two TCP client sockets which will connect to the systems
            gateway.runRaptor("--service=gateway --a-endpoint=tcp --a-role=client --a-remote-host=" + system1.getRaptorHostname() + " --a-remote-port=50000 --a-tls-version=none --b-endpoint=tcp --b-role=client --b-remote-host=" + system2.getRaptorHostname() + " --b-remote-port=50000 --b-tls-version=none");
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
            system1.runRaptor("--service=tcp --role=server --local-port=50000 --tls-version=none --send-strategy=interactive");
            system1.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

            // Start gateway1
            gateway1.runRaptor("--service=gateway --a-endpoint=tcp --a-role=client --a-remote-host=" + system1.getRaptorHostname() + " --a-remote-port=50000 --a-tls-version=none --b-endpoint=tcp --b-role=server --b-local-port=50000 --b-tls-version=none");
            gateway1.expectNumberOfOutputLineContains(1, "connected", system1.getRaptorIpAddress(), "50000");
            gateway1.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

            // Start gateway1
            gateway2.runRaptor("--service=gateway --a-endpoint=tcp --a-role=client --a-remote-host=" + gateway1.getRaptorHostname() + " --a-remote-port=50000 --a-tls-version=none --b-endpoint=tcp --b-role=server --b-local-port=50000 --b-tls-version=none");
            gateway2.expectNumberOfOutputLineContains(1, "connected", gateway1.getRaptorIpAddress(), "50000");
            gateway2.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

            // Start system2
            system2.runRaptor("--service=tcp --role=client --remote-host=" + gateway2.getRaptorHostname() + " --remote-port=50000 --tls-version=none --send-strategy=interactive");
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
