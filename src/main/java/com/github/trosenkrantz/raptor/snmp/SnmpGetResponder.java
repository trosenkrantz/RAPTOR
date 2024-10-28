package com.github.trosenkrantz.raptor.snmp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.auto.reply.StateMachine;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.snmp4j.*;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class SnmpGetResponder {
    private static final Logger LOGGER = Logger.getLogger(SnmpGetResponder.class.getName());

    private static byte[] output;

    public static void run(Configuration configuration) throws IOException, InterruptedException {
        // Read state machine immediately to provide early feedback
        StateMachine stateMachine = new StateMachine(StateMachineConfiguration.readFromFile(configuration.requireString(SnmpService.PARAMETER_SEND_FILE)), out -> output = out);

        UdpAddress address = new UdpAddress("0.0.0.0/" + configuration.requireString(SnmpService.PARAMETER_PORT));
        try (Snmp snmp = new Snmp(new DefaultUdpTransportMapping(address))) {
            snmp.addCommandResponder(new CommandResponder() {
                @Override
                public <A extends Address> void processPdu(CommandResponderEvent<A> event) {
                    PDU pdu = event.getPDU();
                    if (pdu == null) {
                        LOGGER.info("Received null PDU from " + event.getPeerAddress() + ".");
                        return;
                    }

                    if (pdu.getType() != PDU.GET) {
                        LOGGER.info("Received non-get PDU " + pdu + " from " + event.getPeerAddress() + ".");
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
            });

            LOGGER.info("Listing to requests at " + address + "...");
            snmp.listen();
            new CountDownLatch(1).await(); // Wait indefinitely to let agent run
        }
    }

    private static Variable extractOutputVariable() {
        if (output == null) {
            return new Null();
        } else {
            try {
                return AbstractVariable.createFromBER(new BERInputStream(ByteBuffer.wrap(output)));
            } catch (IOException e) {
                ConsoleIo.writeLine("Failed parsing " + BytesFormatter.format(output) + " as Basic Encoding Rules.");
                ConsoleIo.writeException(e);
                return new Null();
            }
        }
    }
}
