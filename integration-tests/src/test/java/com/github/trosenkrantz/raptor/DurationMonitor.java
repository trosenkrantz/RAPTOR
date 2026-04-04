package com.github.trosenkrantz.raptor;

import java.util.concurrent.atomic.AtomicLong;

public class DurationMonitor {
    private static final AtomicLong GLOBAL_MAX = new AtomicLong(0);

    static {
        // Runs exactly once when the JVM (Gradle Executor) exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("Maximum duration waiting for expected output: " + GLOBAL_MAX.get() + "ms")));
    }

    public static void report(long duration) {
        GLOBAL_MAX.accumulateAndGet(duration, Math::max);
    }
}
