package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TcpAutoReplyIntegrationTest extends RaptorIntegrationTest {
    @Test
    public void autoReply() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor client = new Raptor(network)) {
            network.startAll();

            server.runConfiguration("""
                    {
                      "service" : "tcp",
                      "role" : "server",
                      "localPort" : 50000,
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
                        },
                        "commandSubstitutionTimeout": 1000
                      }
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Waiting for client to connect");

            // Client connects to server
            client.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remoteHost": "%s",
                      "remotePort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive",
                      "commandSubstitutionTimeout": 1000
                    }
                    """, server.getRaptorHostname()));
            server.expectNumberOfOutputLineContains(1, "connected", client.getRaptorIpAddress(), "50000");
            client.expectNumberOfOutputLineContains(1, "connected", server.getRaptorIpAddress(), "50000");

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

    @Test
    public void updateConfiguration() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor client = new Raptor(network)) {
            network.startAll();

            server.runConfiguration("""
                    {
                      "service" : "tcp",
                      "role" : "server",
                      "localPort" : 50000,
                      "tlsVersion" : "none",
                      "sendStrategy" : "autoReply",
                      "replies" : {
                        "startState" : "1",
                        "states" : {
                          "1" : [
                            {
                              "input" : "Input 1",
                              "output" : "Output 1"
                            }
                          ]
                        },
                        "commandSubstitutionTimeout": 1000
                      }
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Waiting for client to connect");

            // Client connects to server
            client.runConfiguration(String.format("""
                    {
                      "service": "tcp",
                      "role": "client",
                      "remoteHost": "%s",
                      "remotePort": 50000,
                      "tlsVersion": "none",
                      "sendStrategy": "interactive",
                      "commandSubstitutionTimeout": 1000
                    }
                    """, server.getRaptorHostname()));
            server.expectNumberOfOutputLineContains(1, "connected", client.getRaptorIpAddress(), "50000");
            client.expectNumberOfOutputLineContains(1, "connected", server.getRaptorIpAddress(), "50000");

            client.writeLineToStdIn("Input 1");
            client.expectNumberOfOutputLineContains(1, "received", "text", "Output 1");

            server.setConfiguration("""
                    {
                      "service" : "tcp",
                      "role" : "server",
                      "localPort" : 50000,
                      "tlsVersion" : "none",
                      "sendStrategy" : "autoReply",
                      "replies" : {
                        "startState" : "1",
                        "states" : {
                          "1" : [
                            {
                              "input" : "Input 1",
                              "output" : "Output 2"
                            }
                          ]
                        },
                        "commandSubstitutionTimeout": 1000
                      }
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Updated auto-replies");

            // Client makes server transition state
            client.writeLineToStdIn("Input 1");
            client.expectNumberOfOutputLineContains(1, "received", "text", "Output 2");
        }
    }
}
