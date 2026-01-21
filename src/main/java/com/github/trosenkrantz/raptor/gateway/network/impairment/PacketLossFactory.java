package com.github.trosenkrantz.raptor.gateway.network.impairment;

import com.github.trosenkrantz.raptor.configuration.DoubleSetting;
import com.github.trosenkrantz.raptor.configuration.Setting;

import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class PacketLossFactory implements NetworkImpairmentFactory {
    private static final Logger LOGGER = Logger.getLogger(PacketLossFactory.class.getName());

    public static Setting<Double> SETTING = new DoubleSetting.Builder("p", "packetLoss", "Packet loss Rate", "Chance of packet loss, between 0 and 1")
            .validator(value -> {
                if (value < 0 || value > 1) {
                    return Optional.of("Value must be between 0 and 1, inclusive.");
                }
                return Optional.empty();
            })
            .build();

    private final double chance;
    private final Random random;

    public PacketLossFactory(double chance) {
        this(chance, new Random());
    }

    public PacketLossFactory(double chance, Random random) {
        this.chance = chance;
        this.random = random;
    }

    @Override
    public Consumer<byte[]> create(Consumer<byte[]> consumer) {
        return payload -> {
            if (random.nextDouble() < chance) {
                LOGGER.info("Dropping packet due to simulated loss");
            } else {
                consumer.accept(payload);
            }
        };
    }
}
