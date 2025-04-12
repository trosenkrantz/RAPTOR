package com.github.trosenkrantz.raptor.gateway;

import com.github.trosenkrantz.raptor.udp.UdpEndpointService;

import java.util.Collection;
import java.util.List;

public class EndpointServiceFactory {
    public static Collection<EndpointService> createServices() {
        return List.of(
                new UdpEndpointService()
        );
    }
}
