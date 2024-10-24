package com.github.trosenkrantz.raptor;

import com.github.trosenkrantz.raptor.tcp.TcpService;

import java.util.Collection;
import java.util.List;

public class RaptorFactory {
    public static Collection<RaptorService> createServices() {
        return List.of(new TcpService());
    }
}
