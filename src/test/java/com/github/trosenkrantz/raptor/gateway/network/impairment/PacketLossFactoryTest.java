package com.github.trosenkrantz.raptor.gateway.network.impairment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PacketLossFactoryTest {
    @Test
    void zeroChance() {
        // Arrange
        Random random = new Random(0L);
        int messageLength = 32;
        int messageCount = 32;
        double chance = 0;

        List<byte[]> actual = new ArrayList<>();
        Consumer<byte[]> consumer = new PacketLossFactory(chance).create(actual::add);

        List<byte[]> inputs = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            byte[] input = new byte[messageLength];
            random.nextBytes(input);
            inputs.add(input);
        }

        // Act
        inputs.forEach(consumer);

        // Assert
        Assertions.assertEquals(messageCount, actual.size());
        for (int i = 0; i < messageCount; i++) {
            assertArrayEquals(inputs.get(i), actual.get(i), "Payload with index " + i + " should match expected result");
        }
    }

    @Test
    void someChance() {
        // Arrange
        Random inputRandom = new Random(0L);
        Random packeLossRandom = new Random(0L);
        int messageLength = 32;
        int messageCount = 32;
        int exceptedMessageCount = 26;
        double chance = 0.2;

        List<byte[]> actual = new ArrayList<>();
        Consumer<byte[]> consumer = new PacketLossFactory(chance, packeLossRandom).create(actual::add);

        List<byte[]> inputs = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            byte[] input = new byte[messageLength];
            inputRandom.nextBytes(input);
            inputs.add(input);
        }

        // Act
        inputs.forEach(consumer);

        // Assert
        Assertions.assertEquals(exceptedMessageCount, actual.size());
    }

    @Test
    void flipAllBits() {
        // Arrange
        Random random = new Random(0L);
        int messageLength = 32;
        int messageCount = 32;
        int exceptedMessageCount = 0;
        double chance = 1;

        List<byte[]> actual = new ArrayList<>();
        Consumer<byte[]> consumer = new PacketLossFactory(chance).create(actual::add);

        List<byte[]> inputs = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            byte[] input = new byte[messageLength];
            random.nextBytes(input);
            inputs.add(input);
        }

        // Act
        inputs.forEach(consumer);

        // Assert
        Assertions.assertEquals(exceptedMessageCount, actual.size());
    }
}