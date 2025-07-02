package com.github.trosenkrantz.raptor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.Semaphore;

public abstract class RaptorIntegrationTest {
    private static final Semaphore SEMAPHORE = new Semaphore(Integer.getInteger("concurrent.integration.test.cases")); // Limit concurrent test-cases

    @BeforeEach
    public void acquire() throws InterruptedException {
        SEMAPHORE.acquire();
    }

    @AfterEach
    public void release() {
        SEMAPHORE.release();
    }
}
