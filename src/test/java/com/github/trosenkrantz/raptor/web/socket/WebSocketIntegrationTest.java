package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;

public class WebSocketIntegrationTest {
    @Test
    public void clientConnectsTwice() {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network)
                     .withCommand("--service=web-socket --role=server --port=50000 --tls-version=none --send-strategy=none")
                     .waitingFor(Wait.forLogMessage(".*Waiting for client to connect.*", 1))) {

            server.start(); // Start to get the hostname

            try (Raptor client = new Raptor(network).withCommand("--service=web-socket --role=client --uri=ws\\://" + server.getRaptorHostname() + "\\:50000 --tls-version=none --send-strategy=none")
                    .dependsOn(server)) {

                // Client connects to server
                client.start();
                server.expectNumberOfOutputLineContains(1, "connected", client.getRaptorIpAddress(), "50000");
                client.expectNumberOfOutputLineContains(1, "connected", server.getRaptorIpAddress(), "50000");

                // Client disconnects
                client.stop();
                server.expectNumberOfOutputLineContains(1, "socket closed");

                // Client connects to server again
                client.start();
                server.expectNumberOfOutputLineContains(2, "connected", client.getRaptorIpAddress(), "50000");
                client.expectNumberOfOutputLineContains(2, "connected", server.getRaptorIpAddress(), "50000");
            }
        }
    }

    @Test
    public void clientConnectsWithIpAddress() {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network)
                     .withCommand("--service=web-socket --role=server --port=50000 --tls-version=none --send-strategy=none")
                     .waitingFor(Wait.forLogMessage(".*Waiting for client to connect.*", 1))) {

            server.start(); // Start to get the hostname

            try (Raptor client = new Raptor(network).withCommand("--service=web-socket --role=client --uri=ws\\://" + server.getRaptorIpAddress() + "\\:50000 --tls-version=none --send-strategy=none")
                    .dependsOn(server)) {

                // Client connects to server
                client.start();
                server.expectNumberOfOutputLineContains(1, "connected", client.getRaptorIpAddress(), "50000");
                client.expectNumberOfOutputLineContains(1, "connected", server.getRaptorIpAddress(), "50000");
            }
        }
    }

    @Test
    public void interactive() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network)
                     .withCommand("--service=web-socket --role=server --port=50000 --tls-version=none --send-strategy=interactive")
                     .waitingFor(Wait.forLogMessage(".*Waiting for client to connect.*", 1))) {

            server.start(); // Start to get the hostname

            try (Raptor client = new Raptor(network).withCommand("--service=web-socket --role=client --uri=ws\\://" + server.getRaptorHostname() + "\\:50000 --tls-version=none --send-strategy=interactive")
                    .dependsOn(server)) {

                // Client connects to server
                client.start();
                String serverAddress = server.getRaptorIpAddress();
                String clientAddress = client.getRaptorIpAddress();
                server.expectNumberOfOutputLineContains(1, "connected", serverAddress, clientAddress, "50000");
                client.expectNumberOfOutputLineContains(1, "connected", serverAddress, clientAddress, "50000");

                // Server sends a message to the client
                String textMessage = "Hello, World!";
                server.writeLineToStdIn(textMessage);
                server.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
                client.expectNumberOfOutputLineContains(1, "received", "text", textMessage);

                // Client sends a message to the server
                String binaryMessage = "\\\\x00\\\\x01\\\\x02\\\\x03";
                client.writeLineToStdIn(binaryMessage);
                client.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
                server.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
            }
        }
    }
}
