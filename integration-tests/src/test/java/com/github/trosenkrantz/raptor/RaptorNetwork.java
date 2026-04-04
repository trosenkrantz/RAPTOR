package com.github.trosenkrantz.raptor;

import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a {@link Network} and keeps track of all containers in the network.
 */
public class RaptorNetwork implements AutoCloseable {
    private final Network innerNetwork = Network.newNetwork();
    private final List<Raptor> containers = new ArrayList<>();

    public void addContainer(Raptor container) {
        containers.add(container);
        container.withNetwork(innerNetwork);
    }

    public List<Raptor> getContainers() {
        return containers;
    }

    /**
     * Starts all containers in the network in parallel.
     */
    public void startAll() {
        Startables.deepStart(containers).join();
    }

    @Override
    public void close() {
        innerNetwork.close();
    }
}
