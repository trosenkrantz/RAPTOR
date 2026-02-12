package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SnmpAutoReplyIntegrationTest extends RaptorIntegrationTest {
    @Test
    public void keepAndSwitchStates() throws IOException {
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
                      "version" : "2c",
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
                      "version" : "2c",
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
                      "version" : "2c",
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

    @Test
    public void MultipleBindingsWithMixedResults() throws IOException {
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
                    "startState" : "s1",
                    "states" : {
                      "s1" : [
                        {
                          "input" : "1.2.3.4",
                          "output" : "\\\\x02\\\\x01\\\\x2a",
                          "nextState" : "s2"
                        },
                        {
                          "input" : "1.2.3.5",
                          "output" : "\\\\x04\\\\x05Hello"
                        }
                      ],
                      "s2" : [
                        {
                          "input" : "1.2.3.4",
                          "output" : "\\\\x02\\\\x01\\\\x64"
                        }
                      ]
                    }
                  }
                }
                """);
            agent.expectNumberOfOutputLineContains(1, "Listening to requests"); // Wait until ready to receive

            manager.runConfiguration(String.format("""
                {
                  "service" : "snmp",
                  "role" : "getRequest",
                  "host" : "%s",
                  "port" : 161,
                  "version" : "2c",
                  "community" : "private",
                  "bindings": [
                    { "oid": "1.2.3.4" },
                    { "oid": "1.2.3.5" },
                    { "oid": "1.2.3.6" }
                  ]
                }
                """, agent.getRaptorHostname()));
            manager.expectAnyOutputLineContains(
                    "Received",
                    "1.2.3.4", "42",
                    "1.2.3.5", "Hello",
                    "1.2.3.6", "NULL"
            );

            // Verify state transitioned
            manager.runConfiguration(String.format("""
                {
                  "service" : "snmp",
                  "role" : "getRequest",
                  "host" : "%s",
                  "port" : 161,
                  "version" : "2c",
                  "community" : "private",
                  "bindings": [
                    { "oid": "1.2.3.4" }
                  ]
                }
                """, agent.getRaptorHostname()));
            manager.expectAnyOutputLineContains("Received", "100");
        }
    }

    @Test
    public void firstMatchedOidControlsTransition() throws IOException {
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
                    "startState" : "s1",
                    "states" : {
                      "s1" : [
                        {
                          "input" : "1.2.3.4",
                          "output" : "\\\\x02\\\\x01\\\\x01",
                          "nextState" : "s2"
                        },
                        {
                          "input" : "1.2.3.5",
                          "output" : "\\\\x02\\\\x01\\\\x02",
                          "nextState" : "s3"
                        }
                      ],
                      "s2" : [
                        { "input" : "1.2.3.9", "output" : "\\\\x02\\\\x01\\\\x0a" }
                      ],
                      "s3" : [
                        { "input" : "1.2.3.9", "output" : "\\\\x02\\\\x01\\\\x14" }
                      ]
                    }
                  }
                }
                """);
            agent.expectNumberOfOutputLineContains(1, "Listening to requests");

            manager.runConfiguration(String.format("""
                {
                  "service" : "snmp",
                  "role" : "getRequest",
                  "host" : "%s",
                  "port" : 161,
                  "version" : "2c",
                  "community" : "private",
                  "bindings": [
                    { "oid": "1.2.3.5" },
                    { "oid": "1.2.3.4" }
                  ]
                }
                """, agent.getRaptorHostname()));

            manager.expectAnyOutputLineContains(
                    "Received",
                    "1.2.3.5", "2",
                    "1.2.3.4", "1"
            );

            // Should transition to s3 (first matched OID = 1.2.3.5)
            manager.runConfiguration(String.format("""
                {
                  "service" : "snmp",
                  "role" : "getRequest",
                  "host" : "%s",
                  "port" : 161,
                  "version" : "2c",
                  "community" : "private",
                  "bindings": [
                    { "oid": "1.2.3.9" }
                  ]
                }
                """, agent.getRaptorHostname()));
            manager.expectAnyOutputLineContains("Received", "20");
        }
    }

    @Test
    public void autoReplyMultipleBindingsNoMatches() throws IOException {
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
                    "startState" : "s1",
                    "states" : {
                      "s1" : [
                        {
                          "input" : "1.2.3.4",
                          "output" : "\\\\x02\\\\x01\\\\x2a",
                          "nextState" : "s2"
                        }
                      ]
                    }
                  }
                }
                """);
            agent.expectNumberOfOutputLineContains(1, "Listening to requests");

            manager.runConfiguration(String.format("""
                {
                  "service" : "snmp",
                  "role" : "getRequest",
                  "host" : "%s",
                  "port" : 161,
                  "version" : "2c",
                  "community" : "private",
                  "bindings": [
                    { "oid": "2.9.9.1" },
                    { "oid": "2.9.9.2" }
                  ]
                }
                """, agent.getRaptorHostname()));

            manager.expectAnyOutputLineContains(
                    "Received",
                    "2.9.9.1", "NULL",
                    "2.9.9.2", "NULL"
            );
        }
    }

    @Test
    public void firstMatchedOidDoesNotHaveANextState() throws IOException {
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
                    "startState" : "s1",
                    "states" : {
                      "s1" : [
                        {
                          "input" : "1.2.3.4",
                          "output" : "\\\\x02\\\\x01\\\\x01"
                        },
                        {
                          "input" : "1.2.3.5",
                          "output" : "\\\\x02\\\\x01\\\\x02",
                          "nextState" : "s3"
                        }
                      ],
                      "s2" : [
                        { "input" : "1.2.3.9", "output" : "\\\\x02\\\\x01\\\\x0a" }
                      ],
                      "s3" : [
                        { "input" : "1.2.3.9", "output" : "\\\\x02\\\\x01\\\\x14" }
                      ]
                    }
                  }
                }
                """);
            agent.expectNumberOfOutputLineContains(1, "Listening to requests");

            manager.runConfiguration(String.format("""
                {
                  "service" : "snmp",
                  "role" : "getRequest",
                  "host" : "%s",
                  "port" : 161,
                  "version" : "2c",
                  "community" : "private",
                  "bindings": [
                    { "oid": "1.2.3.4" },
                    { "oid": "1.2.3.5" }
                  ]
                }
                """, agent.getRaptorHostname()));

            manager.expectAnyOutputLineContains(
                    "Received",
                    "1.2.3.4", "1",
                    "1.2.3.5", "2"
            );

            // Should transition to s3, as first match (to s2) have no nextState
            manager.runConfiguration(String.format("""
                {
                  "service" : "snmp",
                  "role" : "getRequest",
                  "host" : "%s",
                  "port" : 161,
                  "version" : "2c",
                  "community" : "private",
                  "bindings": [
                    { "oid": "1.2.3.9" }
                  ]
                }
                """, agent.getRaptorHostname()));
            manager.expectAnyOutputLineContains("Received", "20");
        }
    }
}
