package com.github.trosenkrantz.raptor.serial.port;

import com.github.trosenkrantz.raptor.Raptor;
import com.github.trosenkrantz.raptor.RaptorIntegrationTest;
import com.github.trosenkrantz.raptor.RaptorNetwork;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SerialPortGatewayIntegrationTest extends RaptorIntegrationTest {
    /**
     * raptor1 ttyS1 <-> gateway ttyS1 <-> gateway ttyS2 <-> raptor2 ttyS1.
     */
    @Test
    public void TwoWayGateway() throws IOException {
        try (RaptorNetwork network = new RaptorNetwork();
             Raptor raptor1 = new Raptor(network);
             Raptor gateway = new Raptor(network);
             Raptor raptor2 = new Raptor(network)) {
            network.startAll();

            String portName1 = "ttyS1";
            String portName2 = "ttyS2"; // Gateway needs two ports
            SocatUtility.bridgeSerialPorts(raptor1, portName1, gateway, portName1, "50000");
            SocatUtility.bridgeSerialPorts(gateway, portName2, raptor2, portName1, "50001");

            // Start
            raptor1.runRaptor("--service=serial-port --port=" + portName1 + " --baud-rate=9600 --data-bits=8 --stop-bits=one --parity=no --send-strategy=interactive");
            gateway.runRaptor("--service=gateway --a-endpoint=serial-port --a-port=" + portName1 + " --a-baud-rate=9600 --a-data-bits=8 --a-stop-bits=one --a-parity=no --b-endpoint=serial-port --b-port=" + portName2 + " --b-baud-rate=9600 --b-data-bits=8 --b-stop-bits=one --b-parity=no");
            raptor2.runRaptor("--service=serial-port --port=" + portName1 + " --baud-rate=9600 --data-bits=8 --stop-bits=one --parity=no --send-strategy=interactive");
            raptor1.expectNumberOfOutputLineContains(1, "Listing to " + portName1);
            gateway.expectNumberOfOutputLineContains(1, "Listing to " + portName1);
            gateway.expectNumberOfOutputLineContains(1, "Listing to " + portName2);
            raptor2.expectNumberOfOutputLineContains(1, "Listing to " + portName1);

            // raptor1 sends a message
            String textMessage = "Hello, World!";
            raptor1.writeLineToStdIn(textMessage);
            raptor1.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            gateway.expectNumberOfOutputLineContains(1, "received", "text", textMessage);
            gateway.expectNumberOfOutputLineContains(1, "sent", "text", textMessage);
            raptor2.expectNumberOfOutputLineContains(1, "received", "text", textMessage);

            // raptor2 sends a message
            String binaryMessage = "\\\\x00\\\\x01\\\\x02\\\\x03";
            raptor2.writeLineToStdIn(binaryMessage);
            raptor2.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            gateway.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
            gateway.expectNumberOfOutputLineContains(1, "sent", "bytes", binaryMessage);
            raptor1.expectNumberOfOutputLineContains(1, "received", "bytes", binaryMessage);
        }
    }
}
