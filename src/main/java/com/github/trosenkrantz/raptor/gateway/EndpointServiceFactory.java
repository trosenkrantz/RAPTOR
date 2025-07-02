package com.github.trosenkrantz.raptor.gateway;

import com.github.trosenkrantz.raptor.serial.port.gateway.SerialPortEndpointService;
import com.github.trosenkrantz.raptor.tcp.gateway.TcpEndpointService;
import com.github.trosenkrantz.raptor.udp.gateway.UdpEndpointService;

import java.util.Collection;
import java.util.List;

public class EndpointServiceFactory {
    public static Collection<EndpointService> createServices() {
        return List.of(
                new UdpEndpointService(),
                new SerialPortEndpointService(),
                new TcpEndpointService()
        );
    }
}
