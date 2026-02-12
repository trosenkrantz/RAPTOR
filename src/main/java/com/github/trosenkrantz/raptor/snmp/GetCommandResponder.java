package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.auto.reply.*;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.smi.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetCommandResponder implements CommandResponder {
    private static final Logger LOGGER = Logger.getLogger(GetCommandResponder.class.getName());

    private final PeakableBufferlessStateMachine stateMachine;

    public GetCommandResponder(Configuration configuration) {
        stateMachine = new PeakableBufferlessStateMachine(StateMachineConfiguration.fromConfiguration(configuration));
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
            Transition firstTransition = null;

            for (VariableBinding binding : requestPdu.getVariableBindings()) {
                OID oid = binding.getOid();
                Optional<Transition> optionalTransition = stateMachine.peak(oid.toDottedString().getBytes(StandardCharsets.US_ASCII)); // OIDs are ASCII strings
                responsePDU.add(new VariableBinding(oid, optionalTransition.map(this::extractOutputVariable).orElse(new Null())));

                if (firstTransition == null && optionalTransition.isPresent() && optionalTransition.get().nextState() != null) { // If first transition with a nextState
                    firstTransition = optionalTransition.get();
                }
            }

            if (firstTransition != null) {
                stateMachine.transition(firstTransition);
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

    private Variable extractOutputVariable(Transition transition) {
        try {
            return SnmpService.toVariable(transition.outputAsBytes());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed parsing " + transition.output() + " as Basic Encoding Rules.", e);
            return new Null();
        }
    }
}
