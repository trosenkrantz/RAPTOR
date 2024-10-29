package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.Configuration;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.logging.Logger;

public class SnmpSender {
    private static final Logger LOGGER = Logger.getLogger(SnmpSender.class.getName());

    public static void run(Configuration configuration, int retries, long timeout, PDU pdu) throws IOException {
        DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
        try (Snmp snmp = new Snmp(transport)) {
            transport.listen();

            // Target setup
            CommunityTarget<Address> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("private"));
            UdpAddress address = new UdpAddress(configuration.requireString(SnmpService.PARAMETER_HOST) + "/" + configuration.requireString(SnmpService.PARAMETER_PORT));
            target.setAddress(address);
            target.setRetries(retries);
            target.setTimeout(timeout);
            target.setVersion(configuration.requireEnum(Version.class).getSnmpValue());

            // Send the request
            ResponseEvent<Address> responseEvent = snmp.send(pdu, target);
            String sentMessage = "Sent PDU: " + pdu + " to " + address + ".";
            if (responseEvent != null && responseEvent.getResponse() != null) {
                LOGGER.info(sentMessage + " Received response: " + responseEvent.getResponse());
            } else {
                LOGGER.info(sentMessage + " Did not receive a response.");
            }
        }
    }
}
