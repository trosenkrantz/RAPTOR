package com.github.trosenkrantz.raptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;

class RaptorTest {
    @Test
    void withBasicCommand() {
        // Arrange

        // Act
        String[] actual = Raptor.parseArguments("--service=udp --mode=broadcast --role=receive --local-port=50000");

        // Assert
        Assertions.assertEquals(4, actual.length);
        Assertions.assertEquals("--service=udp", actual[0]);
        Assertions.assertEquals("--mode=broadcast", actual[1]);
        Assertions.assertEquals("--role=receive", actual[2]);
        Assertions.assertEquals("--local-port=50000", actual[3]);
    }

    @Test
    void withQuotedCommand1() {
        // Arrange

        // Act
        String[] actual = Raptor.parseArguments("--service=udp --mode=broadcast --role=send --remote-port=50000 \"--payload=Hello, World\\!\"");

        // Assert
        Assertions.assertEquals(5, actual.length);
        Assertions.assertEquals("--service=udp", actual[0]);
        Assertions.assertEquals("--mode=broadcast", actual[1]);
        Assertions.assertEquals("--role=send", actual[2]);
        Assertions.assertEquals("--remote-port=50000", actual[3]);
        Assertions.assertEquals("--payload=Hello, World\\!", actual[4]);
    }
}