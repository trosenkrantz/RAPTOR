package com.github.trosenkrantz.raptor.gateway.network.impairment;

import com.github.trosenkrantz.raptor.configuration.DoubleSetting;
import com.github.trosenkrantz.raptor.configuration.Setting;

import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class DuplicationFactory implements NetworkImpairmentFactory {
    private static final Logger LOGGER = Logger.getLogger(DuplicationFactory.class.getName());

    public static final Setting<Double> SETTING = new DoubleSetting.Builder("d", "duplication", "Duplication Chance", "Chance of duplicating a message, between 0 and 1. If duplicated, this chance applies again, recursively")
        .validator(value -> {
            if (value < 0 || value >= 1) {
                return Optional.of("Value must be between 0 (inclusive) and 1 (exclusive).");
            }
            return Optional.empty();
        })
        .build();

    private static final int MAX_DUPLICATES = 8;

    private final double chance;
    private final Random random;

    public DuplicationFactory(double chance) {
        this(chance, new Random());
    }

    public DuplicationFactory(double chance, Random random) {
        this.chance = chance;
        this.random = random;
    }

    @Override
    public Consumer<byte[]> create(Consumer<byte[]> consumer) {
        return payload -> {
            consumer.accept(payload); // always send original message

            int duplicationCount = 0;
            while (random.nextDouble() < chance) {
                consumer.accept(payload);
                duplicationCount++;

                if (duplicationCount >= MAX_DUPLICATES) {
                    LOGGER.warning("Maximum duplication limit reached (" + MAX_DUPLICATES + "). Further duplications will not be processed.");
                    break;
                }
            }

            if (duplicationCount > 0) {
                LOGGER.info("Duplicated message " + duplicationCount + " time" + (duplicationCount == 1 ? "" : "s") + ".");
            }
        };
    }
}
