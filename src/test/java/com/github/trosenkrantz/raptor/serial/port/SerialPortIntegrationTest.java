package com.github.trosenkrantz.raptor.serial.port;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

import java.io.IOException;

public class SerialPortIntegrationTest {
    @Test
    public void interactive() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor raptor1 = new Raptor(network);
             Raptor raptor2 = new Raptor(network)) {
            network.startAll();

            raptor1.writeLineToStdIn("socat -d -d TCP-LISTEN:50000,reuseaddr,fork PTY,link=/dev/ttyS1,raw &"); // Map between serial port and TCP server socket
            raptor1.expectNumberOfOutputLineContains(1, "listening"); // Wait for TCP server to open

            raptor2.writeLineToStdIn("socat -d -d PTY,link=/dev/ttyS1,raw TCP:" + raptor1.getRaptorHostname() + ":50000 &"); // Map between serial port and TCP client socket, connecting to raptor1
            raptor1.expectNumberOfOutputLineContains(1, "starting data transfer"); // Wait for socat connection established
            raptor2.expectNumberOfOutputLineContains(1, "starting data transfer");

            raptor1.runRaptor("--service=serial-port --port=ttyS1 --baud-rate=9600 --data-bits=8 --stop-bits=one --parity=no --send-strategy=interactive");
            raptor2.runRaptor("--service=serial-port --port=ttyS1 --baud-rate=9600 --data-bits=8 --stop-bits=one --parity=no --send-strategy=interactive");
            raptor1.expectNumberOfOutputLineContains(1, "Listing to ttyS1");
            raptor2.expectNumberOfOutputLineContains(1, "Listing to ttyS1");

            // raptor1 sends a message to the raptor2
            String textMessage = "Hello, World!";
            raptor1.writeLineToStdIn(textMessage);
            raptor1.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            raptor2.expectNumberOfOutputLineContains(1, "received", "text", textMessage);

            // raptor sends a message to the raptor1
            String binaryMessage = "\\\\x00\\\\x01\\\\x02\\\\x03";
            raptor2.writeLineToStdIn(binaryMessage);
            raptor2.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            raptor1.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
        }
    }
}
