package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SnmpTrapIntegrationTest extends RaptorIntegrationTest {
    @Test
    public void sendTrap() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
                    {
                      "service": "snmp",
                      "role": "listen",
                      "port": 162
                    }
                    """);
            receiver.expectNumberOfOutputLineContains(1, "Listening to requests"); // Wait until ready to receive

            // Act
            sender.runConfiguration(String.format("""
                    {
                      "service": "snmp",
                      "role": "trap",
                      "host": "%s",
                      "port": 162,
                      "version": "2c",
                      "community": "private",
                      "bindings": [
                        {
                          "oid": "1.2.3.4",
                          "variable": "\\\\x04\\\\x05Hello"
                        }
                      ],
                      "commandSubstitutionTimeout": 1000
                    }
                    """, receiver.getRaptorHostname()));

            // Assert
            sender.expectAnyOutputLineContains("Sent", "TRAP", "1.2.3.4", receiver.getRaptorIpAddress(), "Hello", "162");
            receiver.expectAnyOutputLineContains("Received", "TRAP", "1.2.3.4", sender.getRaptorIpAddress(), "Hello");
        }
    }

    @Test
    public void sendTrapVersion1() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
                    {
                      "service": "snmp",
                      "role": "listen",
                      "port": 162
                    }
                    """);
            receiver.expectNumberOfOutputLineContains(1, "Listening to requests"); // Wait until ready to receive

            // Act
            sender.runConfiguration(String.format("""
                    {
                      "service": "snmp",
                      "role": "trap",
                      "host": "%s",
                      "port": 162,
                      "version": "1",
                      "community": "private",
                      "bindings": [
                        {
                          "oid": "1.2.3.4",
                          "variable": "\\\\x04\\\\x05Hello"
                        }
                      ],
                      "commandSubstitutionTimeout": 1000
                    }
                    """, receiver.getRaptorHostname()));

            // Assert
            sender.expectAnyOutputLineContains("Sent", "TRAP", "1.2.3.4", receiver.getRaptorIpAddress(), "162");
            receiver.expectAnyOutputLineContains("Received", "TRAP", "1.2.3.4", sender.getRaptorIpAddress());
        }
    }

    @Test
    public void sendTrapUsingIpAddress() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
                    {
                      "service": "snmp",
                      "role": "listen",
                      "port": 162
                    }
                    """);
            receiver.expectNumberOfOutputLineContains(1, "Listening to requests"); // Wait until ready to receive

            // Act
            sender.runConfiguration(String.format("""
                    {
                      "service": "snmp",
                      "role": "trap",
                      "host": "%s",
                      "port": 162,
                      "version": "2c",
                      "community": "private",
                      "bindings": [
                        {
                          "oid": "1.2.3.4",
                          "variable": "\\\\x04\\\\x05Hello"
                        }
                      ],
                      "commandSubstitutionTimeout": 1000
                    }
                    """, receiver.getRaptorIpAddress()));

            // Assert
            sender.expectAnyOutputLineContains("Sent", "TRAP", "1.2.3.4", receiver.getRaptorIpAddress(), "Hello", "162");
            receiver.expectAnyOutputLineContains("Received", "TRAP", "1.2.3.4", sender.getRaptorIpAddress(), "Hello");
        }
    }

    @Test
    public void sendTrapWithDifferentTypesOfBindings() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor receiver = new Raptor(network);
             Raptor sender = new Raptor(network)) {
            network.startAll();

            // Arrange
            receiver.runConfiguration("""
                    {
                      "service": "snmp",
                      "role": "listen",
                      "port": 162
                    }
                    """);
            receiver.expectNumberOfOutputLineContains(1, "Listening to requests"); // Wait until ready to receive

            // Act
            sender.runConfiguration(String.format("""
                    {
                      "service": "snmp",
                      "role": "trap",
                      "host": "%s",
                      "port": 162,
                      "version": "2c",
                      "community": "private",
                      "bindings": [
                        { "oid": "1.2.3.1", "variable": "\\\\x04\\\\x05Hello" }, // OctetString
                        { "oid": "1.2.3.2", "variable": "\\\\x02\\\\x01\\\\x2A" }, // Integer 42
                        { "oid": "1.2.3.3", "variable": "\\\\x06\\\\x03\\\\x2B\\\\x06\\\\x01" } // OID 1.3.6.1
                      ],
                      "commandSubstitutionTimeout": 1000
                    }
                    """, receiver.getRaptorHostname()));

            // Assert
            sender.expectAnyOutputLineContains("Sent", "TRAP", receiver.getRaptorIpAddress(), "1.2.3.1", "Hello", "1.2.3.2", "42", "1.2.3.3", "1.3.6.1");
            receiver.expectAnyOutputLineContains("Received", "TRAP", sender.getRaptorIpAddress(), "1.2.3.1", "Hello", "1.2.3.2", "42", "1.2.3.3", "1.3.6.1");
        }
    }
}
