package com.github.trosenkrantz.raptor.serial.port.gateway;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.gateway.Endpoint;
import com.github.trosenkrantz.raptor.gateway.EndpointService;
import com.github.trosenkrantz.raptor.serial.port.SerialPortUtility;

import java.io.IOException;
import java.util.function.Consumer;

public class SerialPortEndpointService implements EndpointService {
    @Override
    public String getPromptValue() {
        return "sp";
    }

    @Override
    public String getParameterKey() {
        return "serial-port";
    }

    @Override
    public String getDescription() {
        return "Serial Port";
    }
    @Override
    public void configureEndpoint(Configuration configuration) {
        SerialPortUtility.configureConnectivity(configuration);
    }

    @Override
    public Endpoint createEndpoint(Configuration configuration, Consumer<byte[]> broker, Runnable onEndpointClosed) throws IOException {
        return new SerialPortEndpoint(configuration, broker, onEndpointClosed);
    }
}
