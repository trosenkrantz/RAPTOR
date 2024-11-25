package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.auto.reply.StateMachine;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.smi.*;

import java.io.IOException;
import java.util.logging.Logger;

public class GetCommandResponder implements CommandResponder {
    private static final Logger LOGGER = Logger.getLogger(GetCommandResponder.class.getName());

    private final StateMachine stateMachine;

    private static byte[] output;

    public GetCommandResponder(Configuration configuration) throws IOException {
        stateMachine = new StateMachine(StateMachineConfiguration.readFromFile(configuration.requireString(SnmpService.PARAMETER_SEND_FILE)), out -> output = out);
    }

    @Override
    public <A extends Address> void processPdu(CommandResponderEvent<A> event) {
        PDU pdu = event.getPDU();
        if (pdu == null) {
            LOGGER.info("Received null PDU from " + event.getPeerAddress() + ".");
            return;
        }

        if (pdu.getType() != PDU.GET) {
            LOGGER.info("Received non-GET PDU " + pdu + " from " + event.getPeerAddress() + ".");
            return;
        }

        PDU responsePDU = new PDU();
        responsePDU.setType(PDU.RESPONSE);
        responsePDU.setRequestID(pdu.getRequestID());

        synchronized (this) {
            for (VariableBinding binding : pdu.getVariableBindings()) {
                stateMachine.onInput(binding.getOid().toDottedString().getBytes());
                responsePDU.add(new VariableBinding(binding.getOid(), extractOutputVariable()));
                stateMachine.resetInputBuffer();
                output = null;
            }
        }

        LOGGER.info("Received PDU: " + pdu + " from " + event.getPeerAddress() + ", responding with PDU " + responsePDU + ".");

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
            ConsoleIo.writeException(e);
        }
    }

    private static Variable extractOutputVariable() {
        if (output == null) {
            return new Null();
        } else {
            try {
                return SnmpService.toVariable(output);
            } catch (IOException e) {
                ConsoleIo.writeLine("Failed parsing " + BytesFormatter.bytesToFullyEscapedStringWithType(output) + " as Basic Encoding Rules.");
                ConsoleIo.writeException(e);
                return new Null();
            }
        }
    }
}
