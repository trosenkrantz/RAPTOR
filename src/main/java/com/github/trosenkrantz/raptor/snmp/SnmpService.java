package com.github.trosenkrantz.raptor.snmp;

import com.github.trosenkrantz.raptor.RootService;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.configuration.ObjectListSetting;
import com.github.trosenkrantz.raptor.io.CommandSubstitutor;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SnmpService implements RootService {
    public static final String PARAMETER_HOST = "host";
    public static final String PARAMETER_PORT = "port";
    public static final String PARAMETER_OID = "oid";
    public static final String PARAMETER_COMMUNITY = "community";
    public static final String PARAMETER_VARIABLE = "variable";

    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_OID = "1.2.3.4";
    public static final String DEFAULT_COMMUNITY = "private";
    public static final String DEFAULT_VARIABLE = "\\\\x04\\\\x05Hello";

    private static final ObjectListSetting<VariableBinding> GET_REQUEST_BINDINGS_SETTING = new ObjectListSetting.Builder<>("b", "bindings", "OIDs", "OIDs of MIB variables to request", new OidSettingGroup()).build();
    private static final ObjectListSetting<UnresolvedVariableBinding> SET_REQUEST_BINDINGS_SETTING = new ObjectListSetting.Builder<>("b", "bindings", "Bindings", "MIB variables to set", new VariableBindingSettingGroup()).build();
    private static final ObjectListSetting<UnresolvedVariableBinding> TRAP_BINDINGS_SETTING = new ObjectListSetting.Builder<>("b", "bindings", "OIDs", "MIB variables to send", new VariableBindingSettingGroup()).build();

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
                configuration.setFullyEscapedString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of agent to request", DEFAULT_HOST));
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("Agent IP port to send to", SnmpConstants.DEFAULT_COMMAND_RESPONDER_PORT));
                configuration.setEnum(ConsoleIo.askForOptions(Version.class, Version.V2C));
                configuration.setFullyEscapedString(PARAMETER_COMMUNITY, ConsoleIo.askForString("Community to use", DEFAULT_COMMUNITY));
                GET_REQUEST_BINDINGS_SETTING.configure(configuration);
            }
            case SET_REQUEST -> {
                configuration.setFullyEscapedString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of agent to request", DEFAULT_HOST));
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("Agent IP port to send to", SnmpConstants.DEFAULT_COMMAND_RESPONDER_PORT));
                configuration.setEnum(ConsoleIo.askForOptions(Version.class, Version.V2C));
                configuration.setFullyEscapedString(PARAMETER_COMMUNITY, ConsoleIo.askForString("Community to use", DEFAULT_COMMUNITY));
                SET_REQUEST_BINDINGS_SETTING.configure(configuration);
                CommandSubstitutor.configureTimeout(configuration);
            }
            case RESPOND -> {
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("Local IP port to set up socket for and for managers to send requests to", SnmpConstants.DEFAULT_COMMAND_RESPONDER_PORT));

                StateMachineConfiguration.configureSampleAutoReply(configuration, StateMachineConfiguration.SNMP_REPLIES_PATH);
            }
            case TRAP -> {
                configuration.setFullyEscapedString(PARAMETER_HOST, ConsoleIo.askForString("Hostname / IP address of manager to send trap to", DEFAULT_HOST));
                configuration.setInt(PARAMETER_PORT, ConsoleIo.askForInt("Manager IP port to send to", SnmpConstants.DEFAULT_NOTIFICATION_RECEIVER_PORT));
                configuration.setEnum(ConsoleIo.askForOptions(Version.class, Version.V2C));
                configuration.setFullyEscapedString(PARAMETER_COMMUNITY, ConsoleIo.askForString("Community to use", DEFAULT_COMMUNITY));
                TRAP_BINDINGS_SETTING.configure(configuration);
                CommandSubstitutor.configureTimeout(configuration);
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
                pdu.addAll(GET_REQUEST_BINDINGS_SETTING.readAndRequire(configuration));

                SnmpSender.run(configuration, pdu);
            }
            case SET_REQUEST -> {
                PDU pdu = createPdu(configuration);
                pdu.setType(PDU.SET);
                int commandSubstitutionTimeout = CommandSubstitutor.requireTimeout(configuration);
                pdu.addAll(SET_REQUEST_BINDINGS_SETTING.readAndRequire(configuration).stream().map(binding -> binding.resolve(commandSubstitutionTimeout)).toList());

                SnmpSender.run(configuration, pdu);
            }
            case TRAP -> {
                PDU pdu = createPdu(configuration);
                if (configuration.requireEnum(Version.class) == Version.V1) {
                    pdu.setType(PDU.V1TRAP);
                } else {
                    pdu.setType(PDU.TRAP);
                }

                int commandSubstitutionTimeout = CommandSubstitutor.requireTimeout(configuration);
                pdu.addAll(TRAP_BINDINGS_SETTING.readAndRequire(configuration).stream().map(binding -> binding.resolve(commandSubstitutionTimeout)).toList());

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

    public static Variable toVariable(byte[] berEncoding) throws IOException {
        return AbstractVariable.createFromBER(new BERInputStream(ByteBuffer.wrap(berEncoding)));
    }
}
