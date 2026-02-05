package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SnmpAutoReplyIntegrationTest extends RaptorIntegrationTest {
    @Test
    public void sendTrap() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor agent = new Raptor(network);
             Raptor manager = new Raptor(network)) {
            network.startAll();

            agent.runConfiguration("""
                    {
                      "service" : "snmp",
                      "role" : "respond",
                      "port" : 161,
                      "replies" : {
                        "startState" : "ready1",
                        "states" : {
                          "ready1" : [
                            {
                              "input" : "1.2.3.4",
                              "output" : "\\\\x02\\\\x01\\\\x2a",
                              "nextState" : "ready2"
                            },
                            {
                              "input" : "1.2.3.5..+",
                              "output" : "\\\\x04\\\\x05Hello"
                            }
                          ],
                          "ready2" : [
                            {
                              "input" : "1.2.3.4",
                              "output" : "\\\\x02\\\\x02\\\\x01\\\\x2c",
                              "nextState" : "ready1"
                            }
                          ]
                        }
                      }
                    }
                    """);
            agent.expectNumberOfOutputLineContains(1, "Listening to requests"); // Wait until ready to receive

            // Manager makes server trigger reply while keeping state
            manager.runConfiguration(String.format("""
                    {
                      "service" : "snmp",
                      "role" : "getRequest",
                      "host" : "%s",
                      "port" : 161,
                      "version" : "v2c",
                      "community" : "private",
                      "bindings": [
                        {
                          "oid": "1.2.3.5.1"
                        }
                      ]
                    }
                    """, agent.getRaptorHostname()));
            manager.expectAnyOutputLineContains("Received", "Hello");

            // Manager makes server transition state
            manager.runConfiguration(String.format("""
                    {
                      "service" : "snmp",
                      "role" : "getRequest",
                      "host" : "%s",
                      "port" : 161,
                      "version" : "v2c",
                      "community" : "private",
                      "bindings": [
                        {
                          "oid": "1.2.3.4"
                        }
                      ]
                    }
                    """, agent.getRaptorHostname()));
            manager.expectAnyOutputLineContains("Received", "42");

            // Client makes server trigger reply in new state
            manager.runConfiguration(String.format("""
                    {
                      "service" : "snmp",
                      "role" : "getRequest",
                      "host" : "%s",
                      "port" : 161,
                      "version" : "v2c",
                      "community" : "private",
                      "bindings": [
                        {
                          "oid": "1.2.3.4"
                        }
                      ]
                    }
                    """, agent.getRaptorHostname()));
            manager.expectAnyOutputLineContains("Received", "300");
        }
    }
}
