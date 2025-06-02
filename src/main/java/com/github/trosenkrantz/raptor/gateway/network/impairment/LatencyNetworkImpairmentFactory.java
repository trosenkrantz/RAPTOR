package com.github.trosenkrantz.raptor.gateway.network.impairment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LatencyNetworkImpairmentFactory implements NetworkImpairmentFactory {
    private static final Logger LOGGER = Logger.getLogger(LatencyNetworkImpairmentFactory.class.getName());

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
