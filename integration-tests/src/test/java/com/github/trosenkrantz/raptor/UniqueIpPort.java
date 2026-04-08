package com.github.trosenkrantz.raptor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * As we run tests within the same Docker network (see {@link com.github.trosenkrantz.raptor.RaptorNetwork}), multicast and broadcast traffic might clash across test-cases.
 * To avoid this, this class provides a unique IP port.
 */
public class UniqueIpPort implements AutoCloseable {
    private static final int MIN_PORT = 49152;
    private static final int MAX_PORT = 65535;

    private static final ConcurrentLinkedQueue<Integer> REUSABLE_PORTS = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger NEXT_FRESH_PORT = new AtomicInteger(MIN_PORT);

    private final int port;

    private UniqueIpPort(int port) {
        this.port = port;
    }

    /**
     * Obtains a port from the pool or increments the counter if the pool is empty.
     */
    public static UniqueIpPort claim() {
        Integer reusedPort = REUSABLE_PORTS.poll();
        if (reusedPort != null) {
            return new UniqueIpPort(reusedPort);
        }

        int freshPort = NEXT_FRESH_PORT.getAndIncrement();
        if (freshPort > MAX_PORT) {
            throw new IllegalStateException("No available ports left in the range.");
        }
        
        return new UniqueIpPort(freshPort);
    }

    public int get() {
        return port;
    }

    public String getString() {
        return String.valueOf(port);
    }

    @Override
    public void close() {
        REUSABLE_PORTS.offer(this.port); // Return the port to the queue for the next test case
    }
}