package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.snmp4j.CommandResponder;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.logging.Logger;

public class SnmpListener {
    private static final Logger LOGGER = Logger.getLogger(SnmpListener.class.getName());

    public static void run(Configuration configuration, CommandResponder listener) throws IOException, InterruptedException {
        UdpAddress address = new UdpAddress("0.0.0.0/" + configuration.requireInt(SnmpService.PARAMETER_PORT));
        try (Snmp snmp = new Snmp(new DefaultUdpTransportMapping(address))) {
            snmp.addCommandResponder(listener);

            LOGGER.info("Listening to requests at " + address + "...");
            snmp.listen();

            ConsoleIo.promptUserToExit();
        }
    }
}
