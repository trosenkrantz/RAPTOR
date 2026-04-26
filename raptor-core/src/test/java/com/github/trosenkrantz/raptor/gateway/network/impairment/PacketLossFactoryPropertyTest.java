package com.github.trosenkrantz.raptor.gateway.network.impairment;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

class PacketLossFactoryPropertyTest {
    @Property
    void packetLossZeroProbabilityDropsNothing(@ForAll byte[] payload) {
        PacketLossFactory factory = new PacketLossFactory(0.0, new Random());
        List<byte[]> received = new ArrayList<>();
        Consumer<byte[]> consumer = received::add;
        Consumer<byte[]> impairedConsumer = factory.create(consumer);

        impairedConsumer.accept(payload);

        assertEquals(1, received.size());
        assertArrayEquals(payload, received.get(0));
    }

    @Property
    void packetLossOneProbabilityDropsEverything(@ForAll byte[] payload) {
        PacketLossFactory factory = new PacketLossFactory(1.0, new Random());
        List<byte[]> received = new ArrayList<>();
        Consumer<byte[]> consumer = received::add;
        Consumer<byte[]> impairedConsumer = factory.create(consumer);

        impairedConsumer.accept(payload);

        assertEquals(0, received.size());
    }
}
