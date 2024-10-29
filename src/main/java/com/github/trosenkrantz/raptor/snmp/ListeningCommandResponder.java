package com.github.trosenkrantz.raptor.snmp;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.Address;

import java.util.logging.Logger;

public class ListeningCommandResponder implements CommandResponder {
    private static final Logger LOGGER = Logger.getLogger(ListeningCommandResponder.class.getName());

    @Override
    public <A extends Address> void processPdu(CommandResponderEvent<A> event) {
        PDU pdu = event.getPDU();
        if (pdu == null) {
            LOGGER.info("Received null PDU from " + event.getPeerAddress() + ".");
            return;
        }

        LOGGER.info("Received PDU " + pdu + " from " + event.getPeerAddress() + ".");
    }
}
