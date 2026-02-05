package com.github.trosenkrantz.raptor.auto.reply;

import java.util.function.Consumer;

public class ConcurrentInstance {
    private final byte[] input;
    private final Consumer<byte[]> onOutput;

    public ConcurrentInstance(byte[] input, Consumer<byte[]> onOutput) {
        this.input = input;
        this.onOutput = onOutput;
    }

    public byte[] getInput() {
        return input;
    }

    public Consumer<byte[]> getOnOutput() {
        return onOutput;
    }
}
