package com.github.trosenkrantz.raptor.gateway.network.impairment.bandwidth;

import com.github.trosenkrantz.raptor.configuration.SettingBase;
import com.github.trosenkrantz.raptor.gateway.network.impairment.NetworkImpairmentFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Factory for bandwidth.
 * We use nanoseconds as time unit throughout the class for alignment and high precision.
 * We use the long primitive values instead of boxed objects like Long, Instant, or Duration for easier implementation of thread safety.
 */
public class BandwidthFactory implements NetworkImpairmentFactory {
    private static final Logger LOGGER = Logger.getLogger(BandwidthFactory.class.getName());

    public static final SettingBase<Bandwidth> SETTING = new BandwidthSetting.Builder("b", "bandwidth", "Bandwidth", "Queuing and delaying messages proportional to the amount of data, dropping messages if the queue is full.")
            .build();

    private final long nanosPerByte;
    private final long burstLimit;
    private final long maxQueueDuration;

    private final ScheduledExecutorService executorService;

    /**
     * Timestamp for when we may transmit new messages. If this is in the past, we may immediately burst a new message.
     */
    private long targetTimeNanos = System.nanoTime();

    public BandwidthFactory(Bandwidth bandwidth) {
        this.nanosPerByte = 8L * 1_000_000_000L / bandwidth.bitsPerSecond();
        this.burstLimit = bandwidth.maxBurstDurationMillis() * 1_000_000L;
        this.maxQueueDuration = bandwidth.queueDurationMillis() * 1_000_000L;

        LOGGER.info("Limiting bandwidth to " + bandwidth.bitsPerSecond() + " b/s, with " + bandwidth.maxBurstDurationMillis() + " ms burst limit and " + bandwidth.queueDurationMillis() + " ms maximum queue duration.");

        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public Consumer<byte[]> create(Consumer<byte[]> consumer) {
        return payload -> {
            long scheduleDelay;

            synchronized (this) {
                long now = System.nanoTime();
                long transmissionDuration = payload.length * nanosPerByte;

                // Limit banked bandwidth
                // Example: Time is 2 s, target is 0 s (we have been idle in 2 s), burst limit is 1 s
                // We adjust target to 1 s
                long earliestStartAllowed = now - burstLimit;
                if (targetTimeNanos < earliestStartAllowed) {
                    targetTimeNanos = earliestStartAllowed;
                }

                // Tail drop if transmission would exceed the max queue duration
                // Example:
                // Time is 0 s
                // Current transmission duration is 2 s
                // Target is 1 s (we have 1 s already queued)
                // Max queue duration is 2 s
                // Current message would result in a queue of 3 s, so we drop current message
                if (targetTimeNanos + transmissionDuration > now + maxQueueDuration) {
                    if (transmissionDuration > burstLimit + maxQueueDuration) {
                        LOGGER.warning("Message is too big (" + payload.length + " B) to ever fit within configured bandwidth constrains, dropping it.");
                    } else {
                        LOGGER.info("Dropping message due to bandwidth queue exceeded.");
                    }
                    return;
                }

                targetTimeNanos += transmissionDuration; // For next scheduled transmission

                scheduleDelay = targetTimeNanos - now; // How long from now till we should transmit, allowing negative values
            }

            if (scheduleDelay <= 0) {
                consumer.accept(payload); // Send immediately using banked time
            } else {
                ScheduledFuture<?> unused = executorService.schedule(() -> consumer.accept(payload), scheduleDelay, TimeUnit.NANOSECONDS);
            }
        };
    }
}