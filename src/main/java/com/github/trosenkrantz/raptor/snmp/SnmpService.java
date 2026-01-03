package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;

public class SnmpService implements RootService {
    public static final String PARAMETER_HOST = "host";
    public static final String PARAMETER_PORT = "port";
    public static final String PARAMETER_OID = "oid";
    public static final String PARAMETER_COMMUNITY = "community";
    public static final String PARAMETER_VARIABLE = "variable";;

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_OID = "1.2.3.4";
    private static final String DEFAULT_COMMUNITY = "private";
    private static final String DEFAULT_VARIABLE = "\\\\x04\\\\x05Hello";

    @Override
    public String getPromptValue() {
        return "sn";
    }

    @Override
    public String getParameterKey() {
        return "snmp";
    }

    @Override
    public String getDescription() {
        return "SNMP";
    }

    @Override
    public void configure(Configuration configuration) throws IOException {
        Role role = ConsoleIo.askForOptions(Role.class);
        configuration.setEnum(role);

        switch (role) {
            case GET_REQUEST -> {
                configuration.setString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of agent to request", DEFAULT_HOST));
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("Agent IP port to send to", SnmpConstants.DEFAULT_COMMAND_RESPONDER_PORT));
                configuration.setEnum(ConsoleIo.askForOptions(Version.class, Version.V2C));
                configuration.setString(PARAMETER_OID, ConsoleIo.askForString("OID of MIB variable to request", DEFAULT_OID));
                configuration.setString(PARAMETER_COMMUNITY,  ConsoleIo.askForString("Community to use", DEFAULT_COMMUNITY));
            }
            case SET_REQUEST -> {
                configuration.setString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of agent to request", DEFAULT_HOST));
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("Agent IP port to send to", SnmpConstants.DEFAULT_COMMAND_RESPONDER_PORT));
                configuration.setEnum(ConsoleIo.askForOptions(Version.class, Version.V2C));
                configuration.setString(PARAMETER_OID, ConsoleIo.askForString("OID of MIB variable to set", DEFAULT_OID));
                configuration.setString(PARAMETER_COMMUNITY,  ConsoleIo.askForString("Community to use", DEFAULT_COMMUNITY));
                configuration.setString(PARAMETER_VARIABLE, ConsoleIo.askForString("Variable as escaped string of BER encoding", DEFAULT_VARIABLE));
            }
            case RESPOND -> {
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("Local IP port to set up socket for and for managers to send requests to", SnmpConstants.DEFAULT_COMMAND_RESPONDER_PORT));

                StateMachineConfiguration.configureSampleAutoReply(configuration, StateMachineConfiguration.SNMP_REPLIES_PATH);
            }
            case TRAP -> {
                configuration.setString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of manager to send trap to", DEFAULT_HOST));
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("Manager IP port to send to", SnmpConstants.DEFAULT_NOTIFICATION_RECEIVER_PORT));
                configuration.setEnum(ConsoleIo.askForOptions(Version.class, Version.V2C));
                configuration.setString(PARAMETER_OID, ConsoleIo.askForString("OID of TRAP to send", DEFAULT_OID));
                configuration.setString(PARAMETER_COMMUNITY,  ConsoleIo.askForString("Community to use", DEFAULT_COMMUNITY));
                configuration.setString(PARAMETER_VARIABLE, ConsoleIo.askForString("Variable as escaped string of BER encoding", DEFAULT_VARIABLE));
            }
            case LISTEN -> {
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("Local IP port to set up socket for and for agent to send traps to", SnmpConstants.DEFAULT_NOTIFICATION_RECEIVER_PORT));
            }
        }
    }

    @Override
    public void run(Configuration configuration) throws IOException, InterruptedException {
        switch (configuration.requireEnum(Role.class)) {
            case GET_REQUEST -> {
                PDU pdu = createPdu(configuration);
                pdu.setType(PDU.GET);
                pdu.add(new VariableBinding(new OID(configuration.requireString(PARAMETER_OID))));

                SnmpSender.run(configuration, pdu);
            }
            case SET_REQUEST -> {
                PDU pdu = createPdu(configuration);
                pdu.setType(PDU.SET);
                pdu.add(getVariableBindingWithVariable(configuration));

                SnmpSender.run(configuration, pdu);
            }
            case TRAP -> {
                PDU pdu = createPdu(configuration);
                if (configuration.requireEnum(Version.class) == Version.V1) {
                    pdu.setType(PDU.V1TRAP);
                } else {
                    pdu.setType(PDU.TRAP);
                }
                pdu.add(getVariableBindingWithVariable(configuration));

                SnmpSender.run(configuration, pdu);
            }
            case LISTEN -> {
                SnmpListener.run(configuration, new ListeningCommandResponder());
            }
            case RESPOND -> {
                SnmpListener.run(configuration, new GetCommandResponder(configuration));
            }
        }
    }

    private static VariableBinding getVariableBindingWithVariable(Configuration configuration) throws IOException {
        return new VariableBinding(
                new OID(configuration.requireString(PARAMETER_OID)),
                toVariable(BytesFormatter.fullyEscapedStringToBytes(
                        configuration.requireString(PARAMETER_VARIABLE)
                ))
        );
    }

    private static PDU createPdu(Configuration configuration) {
        return switch (configuration.requireEnum(Version.class)) {
            case V1 -> {
                PDUv1 result = new PDUv1();

                result.setEnterprise(new OID("1.2.3"));
                result.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
                result.setSpecificTrap(1);
                result.setAgentAddress(new IpAddress("127.0.0.1"));

                yield result;
            }
            case V2C -> {
                yield new PDU();
            }
        };
    }

    public static Variable toVariable(byte[] output) throws IOException {
        return AbstractVariable.createFromBER(new BERInputStream(ByteBuffer.wrap(output)));
    }
}
