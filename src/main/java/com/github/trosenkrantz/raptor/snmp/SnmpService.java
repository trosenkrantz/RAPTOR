package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.RaptorService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.snmp4j.PDU;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;

public class SnmpService implements RaptorService {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_OID = "1.2.3.4";
    private static final String DEFAULT_VARIABLE = "\\\\x04\\\\x05Hello";

    public static final String PARAMETER_HOST = "host";
    public static final String PARAMETER_PORT = "port";
    public static final String PARAMETER_OID = "oid";
    public static final String PARAMETER_SEND_FILE = "send-file";
    public static final String PARAMETER_VARIABLE = "variable";

    @Override
    public String getPromptValue() {
        return "s";
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
    public void configure(Configuration configuration) {
        Role role = ConsoleIo.askForOptions(Role.class);
        configuration.setEnum(role);

        Void ignore = switch (role) {
            case GET_REQUEST -> {
                configuration.setString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of agent to request", DEFAULT_HOST));
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("Agent IP port to send to", SnmpConstants.DEFAULT_COMMAND_RESPONDER_PORT)));
                configuration.setEnum(ConsoleIo.askForOptions(Version.class, Version.V2C));
                configuration.setString(PARAMETER_OID, String.valueOf(ConsoleIo.askForString("OID of MIB variable to request", DEFAULT_OID)));

                yield null;
            }
            case SET_REQUEST -> {
                configuration.setString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of agent to request", DEFAULT_HOST));
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("Agent IP port to send to", SnmpConstants.DEFAULT_COMMAND_RESPONDER_PORT)));
                configuration.setEnum(ConsoleIo.askForOptions(Version.class, Version.V2C));
                configuration.setString(PARAMETER_OID, String.valueOf(ConsoleIo.askForString("OID of MIB variable to set", DEFAULT_OID)));
                configuration.setString(PARAMETER_VARIABLE, String.valueOf(ConsoleIo.askForString("Variable as escaped string of BER encoding", DEFAULT_VARIABLE)));

                yield null;
            }
            case RESPOND -> {
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("Local IP port to set up socket for and for managers to send requests to", SnmpConstants.DEFAULT_COMMAND_RESPONDER_PORT)));

                String path = ConsoleIo.askForFile("Absolute or relative path to the auto-reply file", "." + File.separator + "snmp-replies.json");

                // Load state machine immediately to provide early feedback
                try {
                    StateMachineConfiguration stateMachine = StateMachineConfiguration.readFromFile(path);
                    ConsoleIo.writeLine("Parsed file with " + stateMachine.states().keySet().size() + " states and " + stateMachine.states().values().stream().map(List::size).reduce(0, Integer::sum) + " transitions.");
                } catch (IOException e) {
                    ConsoleIo.writeLine("Failed reading file.");
                    throw new UncheckedIOException(e);
                }

                configuration.setString(PARAMETER_SEND_FILE, path);

                yield null;
            }
            case TRAP -> {
                configuration.setString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of manager to send trap to", DEFAULT_HOST));
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("Manager IP port to send to", SnmpConstants.DEFAULT_NOTIFICATION_RECEIVER_PORT)));
                configuration.setEnum(ConsoleIo.askForOptions(Version.class, Version.V2C));
                configuration.setString(PARAMETER_OID, String.valueOf(ConsoleIo.askForString("OID of TRAP to send", DEFAULT_OID)));
                configuration.setString(PARAMETER_VARIABLE, String.valueOf(ConsoleIo.askForString("Variable as escaped string of BER encoding", DEFAULT_VARIABLE)));

                yield null;
            }
            case LISTEN -> {
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("Local IP port to set up socket for and for agent to send traps to", SnmpConstants.DEFAULT_NOTIFICATION_RECEIVER_PORT)));

                yield null;
            }
        };
    }

    @Override
    public void run(Configuration configuration) throws IOException, InterruptedException {
        Void ignore = switch (configuration.requireEnum(Role.class)) {
            case GET_REQUEST -> {
                SnmpSender.run(
                        configuration,
                        2,
                        1000,
                        new PDU(PDU.GET,
                                List.of(new VariableBinding(new OID(configuration.requireString(PARAMETER_OID))))
                        )
                );
                yield null;
            }
            case SET_REQUEST -> {
                SnmpSender.run(
                        configuration,
                        2,
                        1000,
                        new PDU(PDU.SET, List.of(new VariableBinding(
                                new OID(configuration.requireString(PARAMETER_OID)),
                                toVariable(BytesFormatter.fullyEscapedStringToBytes(
                                        configuration.requireString(PARAMETER_VARIABLE)
                                ))
                        )))
                );
                yield null;
            }
            case TRAP -> {
                SnmpSender.run(
                        configuration,
                        SNMP4JSettings.getDefaultRetries(),
                        SNMP4JSettings.getDefaultTimeoutMillis(),
                        new PDU(PDU.TRAP, List.of(new VariableBinding(
                                new OID(configuration.requireString(PARAMETER_OID)),
                                toVariable(BytesFormatter.fullyEscapedStringToBytes(
                                        configuration.requireString(PARAMETER_VARIABLE)
                                ))
                        )))
                );
                yield null;
            }
            case LISTEN -> {
                SnmpListener.run(configuration, new ListeningCommandResponder());
                yield null;
            }
            case RESPOND -> {
                SnmpListener.run(configuration, new GetCommandResponder(configuration));
                yield null;
            }
        };
    }

    public static Variable toVariable(byte[] output) throws IOException {
        return AbstractVariable.createFromBER(new BERInputStream(ByteBuffer.wrap(output)));
    }
}
