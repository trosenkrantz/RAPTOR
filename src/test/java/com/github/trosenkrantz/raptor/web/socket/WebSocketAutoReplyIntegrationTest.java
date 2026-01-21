package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class WebSocketAutoReplyIntegrationTest extends RaptorIntegrationTest {
    @Test
    public void interactive() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor client = new Raptor(network)) {
            network.startAll();

            server.runConfiguration("""
                    {
                      "service" : "webSocket",
                      "role" : "server",
                      "port" : 50000,
                      "tlsVersion" : "none",
                      "sendStrategy" : "autoReply",
                      "replies" : {
                        "startState" : "unauthenticated",
                        "states" : {
                          "unauthenticated" : [
                            {
                              "input" : "AUTH user pass!",
                              "output" : "AUTH_OK!",
                              "nextState" : "ready1"
                            },
                            {
                              "input" : ".*!",
                              "output" : "UNKNOWN_COMMAND!"
                            }
                          ],
                          "ready1" : [
                            {
                              "input" : "STATUS!",
                              "output" : "STATUS:OK!"
                            },
                            {
                              "input" : ".*!",
                              "output" : "UNKNOWN_COMMAND!"
                            }
                          ]
                        }
                      }
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Waiting for client to connect");

            // Client connects to server
            client.runConfiguration(String.format("""
                    {
                      "service": "webSocket",
                      "role": "client",
                      "uri": "ws://%s:50000",
                      "tlsVersion": "none",
                      "sendStrategy": "interactive"
                    }
                    """, server.getRaptorHostname()));
            String serverAddress = server.getRaptorIpAddress();
            String clientAddress = client.getRaptorIpAddress();
            server.expectNumberOfOutputLineContains(1, "connected", serverAddress, clientAddress, "50000");
            client.expectNumberOfOutputLineContains(1, "connected", serverAddress, clientAddress, "50000");

            // Client makes server trigger reply while keeping state
            client.writeLineToStdIn("invalid command!");
            client.expectNumberOfOutputLineContains(1, "received", "text", "UNKNOWN_COMMAND!");

            // Client makes server transition state
            client.writeLineToStdIn("AUTH user pass!");
            client.expectNumberOfOutputLineContains(1, "received", "text", "AUTH_OK!");

            // Client makes server trigger reply in new state
            client.writeLineToStdIn("STATUS!");
            client.expectNumberOfOutputLineContains(1, "received", "text", "STATUS:OK!");
        }
    }
}
