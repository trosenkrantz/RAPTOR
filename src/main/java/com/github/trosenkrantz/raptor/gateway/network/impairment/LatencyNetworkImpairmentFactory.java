package com.github.trosenkrantz.raptor.gateway.network.impairment;

import com.github.trosenkrantz.raptor.configuration.IntegerSetting;
import com.github.trosenkrantz.raptor.configuration.Setting;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LatencyNetworkImpairmentFactory implements NetworkImpairmentFactory {
    private static final Logger LOGGER = Logger.getLogger(LatencyNetworkImpairmentFactory.class.getName());

    public static Setting<Integer> SETTING = new IntegerSetting.Builder("l", "latency", "Latency [ms]", "Latency [ms]")
            .validator(value -> {
                if (value < 0) {
                    return Optional.of("Latency must be a non-negative integer.");
                }
                return Optional.empty();
            })
            .build();

    private final int latency;
    private final ScheduledExecutorService executorService;

    public LatencyNetworkImpairmentFactory(int latency) {
        this.latency = latency;
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public Consumer<byte[]> create(Consumer<byte[]> consumer) {
        return payload -> {
            LOGGER.info("Scheduling message");
            ScheduledFuture<?> unused = executorService.schedule(
                    () -> {
                        try {
                            LOGGER.info("Passing message after introducing latency");
                            consumer.accept(payload);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed passing message after introducing latency.", e);
                        }
                    },
                    latency,
                    TimeUnit.MILLISECONDS
            );
        };
    }
}
