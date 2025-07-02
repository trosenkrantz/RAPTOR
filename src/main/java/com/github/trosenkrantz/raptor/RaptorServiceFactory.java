package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.conversion.ConverterService;
import com.github.trosenkrantz.raptor.gateway.GatewayService;
import com.github.trosenkrantz.raptor.serial.port.SerialPortRootService;
import com.github.trosenkrantz.raptor.snmp.SnmpService;
import com.github.trosenkrantz.raptor.tcp.TcpRootService;
import com.github.trosenkrantz.raptor.udp.UdpRootService;
import com.github.trosenkrantz.raptor.web.socket.WebSocketService;

import java.util.Collection;
import java.util.List;

public class RaptorServiceFactory {
    public static Collection<RootService> createServices() {
        return List.of(
                new UdpRootService(),
                new TcpRootService(),
                new SerialPortRootService(),
                new SnmpService(),
                new WebSocketService(),
                new GatewayService(),
                new ConverterService()
        );
    }
}
