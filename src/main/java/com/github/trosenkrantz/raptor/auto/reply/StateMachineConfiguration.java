package com.github.trosenkrantz.raptor.auto.reply;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public record StateMachineConfiguration(String startState, Map<String, List<Transition>> states) {
    public static final String REPLIES_PATH = "/com/github/trosenkrantz/raptor/replies.json";
    public static final String SNMP_REPLIES_PATH = "/com/github/trosenkrantz/raptor/snmp-replies.json";

    public static StateMachineConfiguration fromConfiguration(final Configuration configuration) {
        StateMachineConfiguration stateMachine = configuration.getObject(AutoRepliesUtility.PARAMETER_REPLIES, StateMachineConfiguration.class);
        ConsoleIo.writeLine("Loaded auto-replies configuration with " + stateMachine.states().size() + " states and " + stateMachine.states().values().stream().map(List::size).reduce(0, Integer::sum) + " transitions.");
        return stateMachine;
    }

    public static void configureSampleAutoReply(Configuration configuration, String resourcePath) throws IOException {
        Configuration autoReplyConfiguration = Configuration.fromStream(StateMachineConfiguration.class.getResourceAsStream(resourcePath));
        configuration.setSubConfiguration(AutoRepliesUtility.PARAMETER_REPLIES, autoReplyConfiguration);

        ConsoleIo.writeLine("Configured sample auto-reply. To adjust: Save configuration, edit it, and re-run RAPTOR.");
    }
}
