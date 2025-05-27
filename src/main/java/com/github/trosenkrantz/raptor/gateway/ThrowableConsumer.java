package com.github.trosenkrantz.raptor.gateway;

import java.io.IOException;

public interface ThrowableConsumer<T> {
    void accept(T payload) throws IOException;
}
