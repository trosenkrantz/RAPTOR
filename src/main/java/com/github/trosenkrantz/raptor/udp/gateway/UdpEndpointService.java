package com.github.trosenkrantz.raptor.udp.gateway;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.gateway.Endpoint;
import com.github.trosenkrantz.raptor.gateway.EndpointService;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.io.IpPortValidator;
import com.github.trosenkrantz.raptor.udp.UdpUtility;

import java.io.IOException;
import java.util.function.Consumer;

public class UdpEndpointService implements EndpointService {
    public static final String PARAMETER_MODE = "mode";
    public static final String PARAMETER_PORT = "port";
    public static final String PARAMETER_LOCAL_SEND_PORT = "local-send-port";

    @Override
    public String getPromptValue() {
        return "u";
    }

    @Override
    public String getParameterKey() {
        return "udp";
    }

    @Override
    public String getDescription() {
        return "UDP, bidirectional";
    }

    @Override
    public void configureEndpoint(Configuration configuration) {
        EndpointMode endpointMode = ConsoleIo.askForOptions(EndpointMode.class);
        configuration.setEnum(PARAMETER_MODE, endpointMode);

        if (endpointMode == EndpointMode.MULTICAST) {
            configuration.setString(UdpUtility.PARAMETER_REMOTE_ADDRESS, ConsoleIo.askForString("Multicast group to use", UdpUtility.DEFAULT_MULTICAST_GROUP));
        }

        configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("Multicast port to send to and receive on", UdpUtility.DEFAULT_PORT, IpPortValidator.VALIDATOR));

        ConsoleIo.askForOptionalInt(
                "Local socket port to bind to when sending",
                "arbitrary ephemeral port",
                IpPortValidator.VALIDATOR
        ).ifPresent(port -> configuration.setInt(PARAMETER_LOCAL_SEND_PORT, port));
    }

    @Override
    public Endpoint createEndpoint(Configuration configuration, Consumer<byte[]> broker, Runnable onEndpointClosed) throws IOException {
        return switch (configuration.requireEnum(UdpEndpointService.PARAMETER_MODE, EndpointMode.class)) {
            case MULTICAST -> new UdpMulticastEndpoint(configuration, broker, onEndpointClosed);
        };
    }
}
