package com.github.trosenkrantz.raptor.gateway.network.impairment;

import com.github.trosenkrantz.raptor.configuration.IntegerInterval;
import com.github.trosenkrantz.raptor.configuration.IntegerIntervalSetting;
import com.github.trosenkrantz.raptor.configuration.Setting;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LatencyFactory implements NetworkImpairmentFactory {
    private static final Logger LOGGER = Logger.getLogger(LatencyFactory.class.getName());

    public static Setting<IntegerInterval> SETTING = new IntegerIntervalSetting.Builder("l", "latency", "Latency [ms]", "Latency [ms]")
            .minValidator(value -> {
                if (value < 0) {
                    return Optional.of("Minimum latency must be a non-negative integer.");
                }
                return Optional.empty();
            }).validator(value -> {
                if (value.min() > value.max()) {
                    return Optional.of("Maximum latency must equal to or larger than minimum latency.");
                }
                return Optional.empty();
            })
            .build();

    private final IntegerInterval latency;
    private final ScheduledExecutorService executorService;
    private final Random random;

    public LatencyFactory(IntegerInterval latency) {
        this(latency, new Random());
    }

    public LatencyFactory(IntegerInterval latency, Random random) {
        this.latency = latency;
        executorService = Executors.newSingleThreadScheduledExecutor();
        this.random = random;
    }

    @Override
    public Consumer<byte[]> create(Consumer<byte[]> consumer) {
        return payload -> {
            int calculatedLatency = getCalculatedLatency(latency.min(), latency.max());
            LOGGER.info("Delaying message with " + calculatedLatency + " ms.");
            ScheduledFuture<?> unused = executorService.schedule(
                    () -> {
                        try {
                            consumer.accept(payload);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed passing message after introducing latency.", e);
                        }
                    },
                    calculatedLatency,
                    TimeUnit.MILLISECONDS
            );
        };
    }

    int getCalculatedLatency(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}
