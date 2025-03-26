package com.github.trosenkrantz.raptor.gateway;

import com.github.trosenkrantz.raptor.udp.UdpService;

import java.util.Collection;
import java.util.List;

public class EndpointServiceFactory {
    public static Collection<EndpointService> createServices() {
        return List.of(
                new UdpService()
        );
    }
}
