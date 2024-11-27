package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.conversion.ConverterService;
import com.github.trosenkrantz.raptor.snmp.SnmpService;
import com.github.trosenkrantz.raptor.tcp.TcpService;
import com.github.trosenkrantz.raptor.web.socket.WebSocketService;

import java.util.Collection;
import java.util.List;

public class RaptorServiceFactory {
    public static Collection<RaptorService> createServices() {
        return List.of(
                new ConverterService(),
                new SnmpService(),
                new TcpService(),
                new WebSocketService()
        );
    }
}
