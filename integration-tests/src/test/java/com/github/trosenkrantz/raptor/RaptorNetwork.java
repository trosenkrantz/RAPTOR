package com.github.trosenkrantz.raptor;

import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a collection of {@link Raptor} containers for a test together to add them to a Docker {@link Network} and start the containers in parallel.
 */
public class RaptorNetwork implements AutoCloseable {
    /**
     * By default, the Docker bridge driver has a limit of 31 concurrent subnets.
     * To avoid exhaustion during high-concurrency test runs, we use a shared Docker network.
     */
    private static final Network DOCKER_NETWORK = Network.newNetwork();

    static {
        // Runs exactly once when the JVM (Gradle Executor) exits
        Runtime.getRuntime().addShutdownHook(new Thread(DOCKER_NETWORK::close));
    }

    private final List<Raptor> containers = new ArrayList<>();

    public void addContainer(Raptor container) {
        containers.add(container);
        container.withNetwork(DOCKER_NETWORK);
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
    }
}
