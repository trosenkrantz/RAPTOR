package com.github.trosenkrantz.raptor.gateway;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.RaptorService;

import java.util.function.Consumer;

/**
 * Service to configure and create endpoints of a specific type.
 * A gateway consists of two endpoints and a broker.
 * An endpoint comments some system with the broker
 */
public interface EndpointService extends RaptorService {
    /**
     * Prompts the user to configure the endpoint.
     *
     * @param configuration configuration to populate with the user's input.
     */
    void configureEndpoint(Configuration configuration);

    /**
     * Creates an endpoint based on the configuration.
     *
     * @param configuration configuration to use for creating the endpoint
     * @param broker        broker for the endpoint to send data to
     * @return the created endpoint
     */
    Endpoint createEndpoint(final Configuration configuration, final Consumer<byte[]> broker);
}
