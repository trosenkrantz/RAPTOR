package com.github.trosenkrantz.raptor.serial.port;

import com.github.trosenkrantz.raptor.Raptor;

import java.io.IOException;

public class SocatUtility {
    public static void bridgeSerialPorts(Raptor raptor1, String raptor1PortName, Raptor raptor2, String raptor2PortName, String tcpPort) throws IOException {
        raptor1.writeLineToStdIn("socat -d -d TCP-LISTEN:" + tcpPort + ",reuseaddr,fork PTY,link=/dev/" + raptor1PortName + ",raw &"); // Map between serial port and TCP server socket
        raptor1.expectNumberOfOutputLineContains(1, "listening"); // Wait for TCP server to open

        raptor2.writeLineToStdIn("socat -d -d PTY,link=/dev/" + raptor2PortName + ",raw TCP:" + raptor1.getRaptorHostname() + ":" + tcpPort + " &"); // Map between serial port and TCP client socket, connecting to raptor1
        raptor1.expectNumberOfOutputLineContains(1, "starting data transfer"); // Wait for socat connection established
        raptor2.expectNumberOfOutputLineContains(1, "starting data transfer");
    }
}
