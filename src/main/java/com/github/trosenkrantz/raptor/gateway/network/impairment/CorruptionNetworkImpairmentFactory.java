package com.github.trosenkrantz.raptor.gateway.network.impairment;

import com.github.trosenkrantz.raptor.configuration.DoubleSetting;
import com.github.trosenkrantz.raptor.configuration.Setting;

import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class CorruptionNetworkImpairmentFactory implements NetworkImpairmentFactory {
    private static final Logger LOGGER = Logger.getLogger(CorruptionNetworkImpairmentFactory.class.getName());

    public static Setting<Double> SETTING = new DoubleSetting.Builder("c", "corruption", "Corruption chance", "Chance of bit flips, between 0 and 1")
            .defaultValue(0.0)
            .validator(value -> {
                if (value < 0 || value > 1) {
                    return Optional.of("Corruption chance must be between 0 and 1.");
                }
                return Optional.empty();
            })
            .build();

    private final double chance;
    private final Random random;

    public CorruptionNetworkImpairmentFactory(double chance) {
        this(chance, new Random());
    }

    public CorruptionNetworkImpairmentFactory(double chance, Random random) {
        this.chance = chance;
        this.random = random;
    }

    @Override
    public Consumer<byte[]> create(Consumer<byte[]> consumer) {
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
