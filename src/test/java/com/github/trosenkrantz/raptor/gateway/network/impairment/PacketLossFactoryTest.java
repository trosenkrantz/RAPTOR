package com.github.trosenkrantz.raptor.gateway.network.impairment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

class PacketLossFactoryTest {
    @Test
    void zeroChance() {
        // Arrange
        int messageLength = 32;
        int messageCount = 32;
        double chance = 0;

        List<byte[]> actual = new ArrayList<>();
        Consumer<byte[]> consumer = new PacketLossFactory(chance).create(actual::add);

        List<byte[]> inputs = MessageGenerator.generateMessages(messageCount, messageLength);

        // Act
        inputs.forEach(consumer);

        // Assert
        Assertions.assertEquals(messageCount, actual.size());
        for (int i = 0; i < messageCount; i++) {
            Assertions.assertArrayEquals(inputs.get(i), actual.get(i), "Payload with index " + i + " should match expected result");
        }
    }

    @Test
    void someChance() {
        // Arrange
        Random random = new Random(0L);
        int messageLength = 32;
        int messageCount = 32;
        int exceptedMessageCount = 26; // Recorded based on deterministic random seed
        double chance = 0.2;

        List<byte[]> actual = new ArrayList<>();
        Consumer<byte[]> consumer = new PacketLossFactory(chance, random).create(actual::add);

        List<byte[]> inputs = MessageGenerator.generateMessages(messageCount, messageLength);

        // Act
        inputs.forEach(consumer);

        // Assert
        Assertions.assertEquals(exceptedMessageCount, actual.size());
    }

    @Test
    void flipAllBits() {
        // Arrange
        int messageLength = 32;
        int messageCount = 32;
        int exceptedMessageCount = 0;
        double chance = 1;

        List<byte[]> actual = new ArrayList<>();
        Consumer<byte[]> consumer = new PacketLossFactory(chance).create(actual::add);

        List<byte[]> inputs = MessageGenerator.generateMessages(messageCount, messageLength);

        // Act
        inputs.forEach(consumer);

        // Assert
        Assertions.assertEquals(exceptedMessageCount, actual.size());
    }
}