package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SnmpIntegrationTest extends RaptorIntegrationTest {
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
              "version": "v2c",
              "oid": "1.2.3.4",
              "community": "private",
              "variable": "\\\\x04\\\\x05Hello"
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
              "version": "v1",
              "oid": "1.2.3.4",
              "community": "private",
              "variable": "\\\\x04\\\\x05Hello"
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
              "version": "v2c",
              "oid": "1.2.3.4",
              "community": "private",
              "variable": "\\\\x04\\\\x05Hello"
            }
            """, receiver.getRaptorIpAddress()));

            // Assert
            sender.expectAnyOutputLineContains("Sent", "TRAP", "1.2.3.4", receiver.getRaptorIpAddress(), "Hello", "162");
            receiver.expectAnyOutputLineContains("Received", "TRAP", "1.2.3.4", sender.getRaptorIpAddress(), "Hello");
        }
    }
}
