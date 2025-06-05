package com.github.trosenkrantz.raptor.gateway.network.impairment;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CorruptionNetworkImpairmentFactory implements NetworkImpairmentFactory {
    private static final Logger LOGGER = Logger.getLogger(CorruptionNetworkImpairmentFactory.class.getName());

    private final double chance;

    public CorruptionNetworkImpairmentFactory(double chance) {
        this.chance = chance;
    }

    @Override
    public Consumer<byte[]> create(Consumer<byte[]> consumer) {
        Random random = new Random();

        return payload -> {
            int count = 0;

            for (int i = 0; i < payload.length; i++) {
                byte aByte = payload[i];
                for (int bitPosition = 0; bitPosition < 8; bitPosition++) {
                    if (random.nextDouble() < chance) {
                        aByte ^= (byte) (1 << bitPosition); // flip bit
                        count++;
                    }
                }
                payload[i] = aByte;
            }

            if (count > 0) {
                LOGGER.info("Flipped " + count + " bits");
            }

            consumer.accept(payload);
        };
    }
}
