package com.github.trosenkrantz.raptor.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class DelayedConsumer<T> implements Consumer<T> {
    private final List<T> queue = new ArrayList<>();
    private final AtomicReference<Consumer<T>> delegate = new AtomicReference<>(queue::add);

    @Override
    public synchronized void accept(T t) {
        delegate.get().accept(t);
    }

    public synchronized void setDelegate(Consumer<T> delegate) {
        queue.forEach(delegate); // Flush the queue
        this.delegate.set(delegate);
    }
}
