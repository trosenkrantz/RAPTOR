package com.github.trosenkrantz.raptor.gateway.network.impairment;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorruptionNetworkImpairmentFactoryTest {
    @Test
    void zeroChance() {
        // Arrange
        byte[] input = new byte[]{0, 1, 2, 3};
        byte[] expected = new byte[]{0, 1, 2, 3};
        double chance = 0;

        CorruptionNetworkImpairmentFactory factory = new CorruptionNetworkImpairmentFactory(chance);
        AtomicReference<byte[]> actual = new AtomicReference<>();
        Consumer<byte[]> consumer = factory.create(actual::set);

        // Act
        consumer.accept(input);

        // Assert
        assertNotNull(actual.get(), "Consumer should have been called");
        assertArrayEquals(expected, actual.get(), "Payload should match expected corrupted result");
    }

    @Test
    void someChance() {
        // Arrange
        byte[] input = new byte[]{0, 1, 2, 3};
        byte[] expected = new byte[]{0, 57, 3, 51};
        double chance = 0.2;
        Random deterministicRandom = new Random(0L);

        CorruptionNetworkImpairmentFactory factory = new CorruptionNetworkImpairmentFactory(chance, deterministicRandom);
        AtomicReference<byte[]> actual = new AtomicReference<>();
        Consumer<byte[]> consumer = factory.create(actual::set);

        // Act
        consumer.accept(input);

        // Assert
        assertNotNull(actual.get(), "Consumer should have been called");
        assertArrayEquals(expected, actual.get(), "Payload should match expected corrupted result");
    }

    @Test
    void flipAllBits() {
        // Arrange
        byte[] input = new byte[]{0, 1, 2, 3};
        byte[] expected = new byte[]{-1, -2, -3, -4};
        double chance = 1;

        CorruptionNetworkImpairmentFactory factory = new CorruptionNetworkImpairmentFactory(chance);
        AtomicReference<byte[]> actual = new AtomicReference<>();
        Consumer<byte[]> consumer = factory.create(actual::set);

        // Act
        consumer.accept(input);

        // Assert
        assertNotNull(actual.get(), "Consumer should have been called");
        assertArrayEquals(expected, actual.get(), "Payload should match expected corrupted result");
    }
}