package com.github.trosenkrantz.raptor.gateway.network.impairment;

import com.github.trosenkrantz.raptor.configuration.DoubleSetting;
import com.github.trosenkrantz.raptor.configuration.SettingBase;

import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class CorruptionFactory implements NetworkImpairmentFactory {
    private static final Logger LOGGER = Logger.getLogger(CorruptionFactory.class.getName());

    public static SettingBase<Double> SETTING = new DoubleSetting.Builder("c", "corruption", "Corruption Rate", "Chance of bit flips, between 0 and 1")
            .validator(value -> {
                if (value < 0 || value > 1) {
                    return Optional.of("Value must be between 0 and 1, inclusive.");
                }
                return Optional.empty();
            })
            .build();

    private final double chance;
    private final Random random;

    public CorruptionFactory(double chance) {
        this(chance, new Random());
    }

    public CorruptionFactory(double chance, Random random) {
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
