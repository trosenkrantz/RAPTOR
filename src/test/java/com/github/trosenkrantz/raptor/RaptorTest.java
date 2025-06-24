package com.github.trosenkrantz.raptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;

class RaptorTest {
    @Test
    void withBasicCommand() {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor raptor = new Raptor(network)
                     .withCommand("--mode=broadcast --local-port=50000 --role=receive --service=udp")) {

            Assertions.assertEquals(4, raptor.getCommandParts().length);
            Assertions.assertEquals("--mode=broadcast", raptor.getCommandParts()[0]);
            Assertions.assertEquals("--local-port=50000", raptor.getCommandParts()[1]);
            Assertions.assertEquals("--role=receive", raptor.getCommandParts()[2]);
            Assertions.assertEquals("--service=udp", raptor.getCommandParts()[3]);
        }
    }

    @Test
    void withQuotedCommand1() {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor raptor = new Raptor(network)
                     .withCommand("--mode=broadcast --role=send \"--payload=Hello, World\\!\" --service=udp --remote-port=50000")) {

            Assertions.assertEquals(5, raptor.getCommandParts().length);
            Assertions.assertEquals("--mode=broadcast", raptor.getCommandParts()[0]);
            Assertions.assertEquals("--role=send", raptor.getCommandParts()[1]);
            Assertions.assertEquals("--payload=Hello, World\\!", raptor.getCommandParts()[2]);
            Assertions.assertEquals("--service=udp", raptor.getCommandParts()[3]);
            Assertions.assertEquals("--remote-port=50000", raptor.getCommandParts()[4]);
        }
    }
}