package com.github.trosenkrantz.raptor.io;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TcpAutoReplyIntegrationTest extends RaptorIntegrationTest {
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

    @Test
    public void commandSubstitution() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor client = new Raptor(network)) {
            network.startAll();

            // Start a server that uses command substitution in its auto-reply
            server.runConfiguration("""
                    {
                      "service" : "tcp",
                      "role" : "server",
                      "localPort" : 50000,
                      "tlsVersion" : "none",
                      "sendStrategy" : "autoReply",
                      "replies" : {
                        "startState" : "A",
                        "states" : {
                          "A" : [
                            {
                              "input" : "GET_TIME!",
                              "output" : "TIME: \\\\$(echo 12:00:00)!"
                            }
                          ]
                        },
                        "commandSubstitutionTimeout": 1000
                      }
                    }
                    """);
            server.expectNumberOfOutputLineContains(1, "Waiting for client to connect", "50000");

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

            // Client sends request that triggers command substitution
            client.writeLineToStdIn("GET_TIME!");

            // Verify that the command substitution was resolved correctly
            client.expectNumberOfOutputLineContains(1, "received", "text", "TIME: 12:00:00!");
        }
    }
}
