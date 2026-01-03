package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.auto.reply.StateMachine;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import org.snmp4j.*;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.smi.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetCommandResponder implements CommandResponder {
    private static final Logger LOGGER = Logger.getLogger(GetCommandResponder.class.getName());

    private final StateMachine stateMachine;

    private byte[] output;

    public GetCommandResponder(Configuration configuration) throws IOException {
        stateMachine = new StateMachine(StateMachineConfiguration.fromConfiguration(configuration), out -> output = out);
    }

    @Override
    public <A extends Address> void processPdu(CommandResponderEvent<A> event) {
        PDU requestPdu = event.getPDU();
        if (requestPdu == null) {
            LOGGER.info("Received null PDU from " + event.getPeerAddress() + ".");
            return;
        }

        if (requestPdu.getType() != PDU.GET) {
            LOGGER.info("Received non-GET PDU " + requestPdu + " from " + event.getPeerAddress() + ".");
            return;
        }

        PDU responsePDU = new PDU();
        responsePDU.setType(PDU.RESPONSE);
        responsePDU.setRequestID(requestPdu.getRequestID());

        synchronized (this) {
            for (VariableBinding binding : requestPdu.getVariableBindings()) {
                stateMachine.onInput(binding.getOid().toDottedString().getBytes(StandardCharsets.US_ASCII)); // OIDs are ASCII strings
                responsePDU.add(new VariableBinding(binding.getOid(), extractOutputVariable()));
                stateMachine.resetInputBuffer();
                output = null;
            }
        }

        LOGGER.info("Received " + SnmpUtility.pduToString(event, requestPdu) + " from " + event.getPeerAddress() + ", responding with " + SnmpUtility.pduToString(event, responsePDU) + ".");

        try {
            event.getMessageDispatcher().returnResponsePdu(
                    event.getMessageProcessingModel(),
                    event.getSecurityModel(),
                    event.getSecurityName(),
                    event.getSecurityLevel(),
                    responsePDU,
                    event.getMaxSizeResponsePDU(),
                    event.getStateReference(),
                    new StatusInformation()
            );
        } catch (MessageException e) {
            LOGGER.log(Level.SEVERE, "Failed to send response PDU", e);
        }
    }

    private Variable extractOutputVariable() {
        if (output == null) {
            return new Null();
        } else {
            try {
                return SnmpService.toVariable(output);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed parsing " + BytesFormatter.bytesToFullyEscapedStringWithType(output) + " as Basic Encoding Rules.", e);
                return new Null();
            }
        }
    }
}
