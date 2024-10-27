package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.Configuration;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.logging.Logger;

public class SnmpGetRequester {
    private static final Logger LOGGER = Logger.getLogger(SnmpGetRequester.class.getName());

    public static void run(Configuration configuration) throws IOException {
        DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
        try (Snmp snmp = new Snmp(transport)) {
            transport.listen();

            // Target setup
            CommunityTarget<Address> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("private"));
            UdpAddress address = new UdpAddress(configuration.requireString(SnmpService.PARAMETER_HOST) + "/" + configuration.requireString(SnmpService.PARAMETER_PORT));
            target.setAddress(address);
            target.setRetries(2);
            target.setTimeout(1000);
            target.setVersion(SnmpConstants.version2c);

            // Create a PDU for the request
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(configuration.requireString(SnmpService.PARAMETER_OID))));
            pdu.setType(PDU.GET);

            // Send the request
            ResponseEvent<Address> responseEvent = snmp.send(pdu, target);
            LOGGER.info("Sent request: " + pdu + " to " + address + ".");
            if (responseEvent != null && responseEvent.getResponse() != null) {
                LOGGER.info("Received response: " + responseEvent.getResponse());
            } else {
                LOGGER.info("Did not receive a response.");
            }
        }
    }
}
