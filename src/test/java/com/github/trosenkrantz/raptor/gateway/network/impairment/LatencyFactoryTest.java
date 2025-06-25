package com.github.trosenkrantz.raptor.gateway.network.impairment;

import com.github.trosenkrantz.raptor.configuration.IntegerInterval;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

class LatencyFactoryTest {
    @Test
    void calculateInclusiveInterval() {
        // Arrange
        int min = 2;
        int max = 4;
        Random random = new Random(0L);
        int tries = 32;

        LatencyFactory latencyFactory = new LatencyFactory(new IntegerInterval(min, max), random);

        // Act and Assert
        for (int i = 0; i < tries; i++) {
            int actual = latencyFactory.getCalculatedLatency(min, max);
            Assertions.assertTrue(actual >= min && actual <= max, "Calculated latency must be within the inclusive range [" + min + ", " + max + "]");
        }
    }
}