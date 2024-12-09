package com.github.trosenkrantz.raptor;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}