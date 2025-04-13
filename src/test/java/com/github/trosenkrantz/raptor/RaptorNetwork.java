package com.github.trosenkrantz.raptor;

import org.testcontainers.containers.Network;

import java.util.ArrayList;
import java.util.List;

public class RaptorNetwork implements AutoCloseable {
    private final Network innerNetwork = Network.newNetwork();
    private final List<Raptor> containers = new ArrayList<>();

    public Network getInnerNetwork() {
        return innerNetwork;
    }

    public void addContainer(Raptor container) {
        containers.add(container);
        container.withNetwork(innerNetwork);
    }

    public List<Raptor> getContainers() {
        return containers;
    }

    @Override
    public void close() {
        innerNetwork.close();
    }
}
