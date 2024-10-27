package com.github.trosenkrantz.raptor.snmp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.RaptorService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.snmp4j.mp.SnmpConstants;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public class SnmpService implements RaptorService {
    private static final int DEFAULT_PORT = SnmpConstants.DEFAULT_COMMAND_RESPONDER_PORT;
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_OID = "1.2.3.4";

    public static final String PARAMETER_HOST = "host";
    public static final String PARAMETER_PORT = "port";
    public static final String PARAMETER_OID = "oid";
    public static final String PARAMETER_SEND_FILE = "send-file";

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
        Role role = ConsoleIo.askForOptions(Role.getPromptOptions());
        configuration.setEnum(role);

        Void ignore = switch (role) {
            case GET_REQUEST -> {
                configuration.setString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of agent to request", DEFAULT_HOST));
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("IP port of agent", DEFAULT_PORT)));
                configuration.setString(PARAMETER_OID, String.valueOf(ConsoleIo.askForString("OID of MIB variable to request", DEFAULT_OID)));

                yield null;
            }
            case GET_RESPOND -> {
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("local IP port to set up socket for and for managers to send requests to", DEFAULT_PORT)));

                String path = ConsoleIo.askForFile("Absolute or relative path to the auto-reply file", "." + File.separator + "reply.json");

                // Load state machine immediately to provide early feedback
                try {
                    StateMachineConfiguration stateMachine = new ObjectMapper().readValue(new File(path), StateMachineConfiguration.class);
                    ConsoleIo.writeLine("Parsed file with " + stateMachine.states().keySet().size() + " states and " + stateMachine.states().values().stream().map(List::size).reduce(0, Integer::sum) + " transitions.");
                } catch (IOException e) {
                    ConsoleIo.writeLine("Failed reading file.");
                    throw new UncheckedIOException(e);
                }

                configuration.setString(PARAMETER_SEND_FILE, path);

                yield null;
            }
            case SUBSCRIBE -> {
                configuration.setString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of agent to subscribe at", DEFAULT_HOST));
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("IP port of agent", DEFAULT_PORT)));

                yield null;
            }
            case TRAP -> {
                configuration.setString(PARAMETER_PORT, String.valueOf(ConsoleIo.askForInt("local IP port to set up sokcet for and for managers to subscribe at", DEFAULT_PORT)));

                yield null;
            }
        };
    }

    @Override
    public void run(Configuration configuration) throws IOException, InterruptedException {
        // TODO Versions?

        Void ignore = switch (configuration.requireEnum(Role.class)) {
            case GET_REQUEST -> {
                SnmpGetRequester.run(configuration);
                yield null;
            }
            case GET_RESPOND -> {
                SnmpGetResponder.run(configuration);
                yield null;
            }
            case SUBSCRIBE -> null;
            case TRAP -> null;
        };
    }
}
