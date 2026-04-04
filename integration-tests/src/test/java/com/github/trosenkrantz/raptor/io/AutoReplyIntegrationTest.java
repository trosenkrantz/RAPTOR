package com.github.trosenkrantz.raptor.io;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class AutoReplyIntegrationTest extends RaptorIntegrationTest {
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
        testSingleAutoReply(
                "PROCESS!",
                "ACK: \\\\$(echo Hello, World!)!",
                "PROCESS!",
                "ACK: Hello, World!!"
        );
    }

    @Test
    public void captureGroupZero() throws IOException {
        testSingleAutoReply(
                "PROCESS: .*!",
                "ACK: \\\\{0}!",
                "PROCESS: Hello, World!",
                "ACK: PROCESS: Hello, World!!"
        );
    }

    @Test
    public void captureGroupZeroInCommandSubstitution() throws IOException {
        testSingleAutoReply(
                "PROCESS: .*!",
                "REVERSED: \\\\$(echo \\\\{0} | rev)!",
                "PROCESS: Hello, World!",
                "REVERSED: !dlroW ,olleH :SSECORP!"
        );
    }

    @Test
    public void captureGroupOne() throws IOException {
        testSingleAutoReply(
                "PROCESS: (.*)!",
                "ACK: \\\\{1}!",
                "PROCESS: Hello, World!",
                "ACK: Hello, World!"
        );
    }

    @Test
    public void captureGroupOneInsideCommandSubstitution() throws IOException {
        testSingleAutoReply(
                "PROCESS: (.*)!",
                "REVERSED: \\\\$(echo \\\\{1} | rev)!",
                "PROCESS: Hello, World!",
                "REVERSED: dlroW ,olleH!"
        );
    }

    @Test
    public void multipleCaptureGroups() throws IOException {
        testSingleAutoReply(
                "PROCESS: (\\\\d+) (\\\\d+)!",
                "RESULT: \\\\$(echo \\\\{1} + \\\\{2} | bc)!", // Adding two numbers
                "PROCESS: 15 27!",
                "RESULT: 42!"
        );
    }

    public void testSingleAutoReply(String replyInput, String replyOutput, String toSend, String expectedReceived) throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor server = new Raptor(network);
             Raptor client = new Raptor(network)) {
            network.startAll();

            server.runConfiguration(String.format("""
                    {
                      "service" : "tcp",
                      "role" : "server",
                      "localPort" : 50000,
                      "tlsVersion" : "none",
                      "sendStrategy" : "autoReply",
                      "replies" : {
                        "startState" : "active",
                        "states" : {
                          "active" : [
                            {
                              "input" : "%s",
                              "output" : "%s"
                            }
                          ]
                        },
                        "commandSubstitutionTimeout": 1000
                      }
                    }
                    """, replyInput, replyOutput));
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

            // Wait for mutual connection
            server.expectNumberOfOutputLineContains(1, "connected", client.getRaptorIpAddress(), "50000");
            client.expectNumberOfOutputLineContains(1, "connected", server.getRaptorIpAddress(), "50000");

            client.writeLineToStdIn(toSend);

            client.expectNumberOfOutputLineContains(1, "received", expectedReceived);
        }
    }
}
