package com.github.trosenkrantz.raptor.tcp.gateway;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.gateway.Endpoint;
import com.github.trosenkrantz.raptor.gateway.EndpointService;
import com.github.trosenkrantz.raptor.tcp.TcpUtility;

import java.io.IOException;
import java.util.function.Consumer;

public class TcpEndpointService implements EndpointService {
    @Override
    public String getPromptValue() {
        return "t";
    }

    @Override
    public String getParameterKey() {
        return "tcp";
    }

    @Override
    public String getDescription() {
        return "TCP";
    }

    @Override
    public void configureEndpoint(Configuration configuration) {
        TcpUtility.configureConnectivity(configuration);
    }

    @Override
    public Endpoint createEndpoint(Configuration configuration, Consumer<byte[]> broker, Runnable onEndpointClosed) throws IOException {
        return new TcpEndpoint(configuration, broker, onEndpointClosed);
    }
}
