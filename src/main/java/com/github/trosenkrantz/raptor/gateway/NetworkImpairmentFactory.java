package com.github.trosenkrantz.raptor.gateway;

import java.util.function.Consumer;

public interface NetworkImpairmentFactory {
    Consumer<byte[]> create(Consumer<byte[]> consumer);
}
