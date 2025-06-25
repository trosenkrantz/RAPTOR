package com.github.trosenkrantz.raptor.gateway.network.impairment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class CorruptionFactoryTest {
    @Test
    void zeroChance() {
        // Arrange
        byte[] input = new byte[]{0, 1, 2, 3};
        byte[] expected = new byte[]{0, 1, 2, 3};
        double chance = 0;

        AtomicReference<byte[]> actual = new AtomicReference<>();
        Consumer<byte[]> consumer = new CorruptionFactory(chance).create(actual::set);

        // Act
        consumer.accept(input);

        // Assert
        Assertions.assertNotNull(actual.get());
        Assertions.assertArrayEquals(expected, actual.get());
    }

    @Test
    void someChance() {
        // Arrange
        byte[] input = new byte[]{0, 1, 2, 3};
        byte[] expected = new byte[]{0, 57, 3, 51};
        double chance = 0.2;
        Random random = new Random(0L);

        AtomicReference<byte[]> actual = new AtomicReference<>();
        Consumer<byte[]> consumer = new CorruptionFactory(chance, random).create(actual::set);

        // Act
        consumer.accept(input);

        // Assert
        Assertions.assertNotNull(actual.get());
        Assertions.assertArrayEquals(expected, actual.get());
    }

    @Test
    void flipAllBits() {
        // Arrange
        byte[] input = new byte[]{0, 1, 2, 3};
        byte[] expected = new byte[]{-1, -2, -3, -4};
        double chance = 1;

        AtomicReference<byte[]> actual = new AtomicReference<>();
        Consumer<byte[]> consumer = new CorruptionFactory(chance).create(actual::set);

        // Act
        consumer.accept(input);

        // Assert
        Assertions.assertNotNull(actual.get());
        Assertions.assertArrayEquals(expected, actual.get());
    }
}