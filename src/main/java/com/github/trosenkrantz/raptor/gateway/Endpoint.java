package com.github.trosenkrantz.raptor.gateway;

import java.io.IOException;

/**
 * A gateway has two endpoints and a broker.
 * An endpoint is interfacing with an external system.
 */
public interface Endpoint {
    void sendToExternalSystem(byte[] payload) throws IOException;
}
