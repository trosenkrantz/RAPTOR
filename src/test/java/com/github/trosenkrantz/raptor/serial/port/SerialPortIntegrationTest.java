package com.github.trosenkrantz.raptor.serial.port;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SerialPortIntegrationTest extends RaptorIntegrationTest {
    @Test
    @Disabled("Due to socat bridging being flaky.")
    public void interactive() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor raptor1 = new Raptor(network);
             Raptor raptor2 = new Raptor(network)) {
            network.startAll();

            String portName = "ttyS1";
            SocatUtility.bridgeSerialPorts(raptor1, portName, raptor2, portName, "50000");

            // Start
            raptor1.runRaptor("--service=serial-port --port=" + portName + " --baud-rate=9600 --data-bits=8 --stop-bits=one --parity=no --send-strategy=interactive");
            raptor2.runRaptor("--service=serial-port --port=" + portName + " --baud-rate=9600 --data-bits=8 --stop-bits=one --parity=no --send-strategy=interactive");
            raptor1.expectNumberOfOutputLineContains(1, "Listing to " + portName);
            raptor2.expectNumberOfOutputLineContains(1, "Listing to " + portName);

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
