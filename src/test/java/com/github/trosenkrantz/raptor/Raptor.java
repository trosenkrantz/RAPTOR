package com.github.trosenkrantz.raptor;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Raptor extends GenericContainer<Raptor> {
    public Raptor(final Network network) {
        super("raptor:latest");

        withNetwork(network)
                .withCreateContainerCmdModifier(cmd -> cmd.withTty(true));
    }

    public void assertOutput(String expected) throws TimeoutException {
        WaitingConsumer consumer = new WaitingConsumer();
        followOutput(consumer, OutputFrame.OutputType.STDOUT);
        consumer.waitUntil(frame -> frame.getUtf8String().contains(expected), 30, TimeUnit.SECONDS);
    }
}
