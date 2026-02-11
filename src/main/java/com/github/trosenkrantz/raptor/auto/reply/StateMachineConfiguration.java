package com.github.trosenkrantz.raptor.auto.reply;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class StateMachineConfiguration {
    public static final String REPLIES_PATH = "/com/github/trosenkrantz/raptor/replies.json";
    public static final String SNMP_REPLIES_PATH = "/com/github/trosenkrantz/raptor/snmp-replies.json";

    private static final Logger LOGGER = Logger.getLogger(StateMachineConfiguration.class.getName());

    private String startState;
    private Map<String, List<Transition>> states;

    public StateMachineConfiguration() {
    }

    public StateMachineConfiguration(String startState, Map<String, List<Transition>> states) {
        this.startState = startState;
        this.states = states;
    }

    public static StateMachineConfiguration fromConfiguration(final Configuration configuration) {
        StateMachineConfiguration originalStateMachine = configuration.requireObject(AutoRepliesUtility.PARAMETER_REPLIES, StateMachineConfiguration.class);
        ConsoleIo.writeLine("Loaded auto-replies configuration with " + originalStateMachine.states().size() + " states and " + originalStateMachine.states().values().stream().map(List::size).reduce(0, Integer::sum) + " transitions.");

        boolean subscribed = configuration.subscribeToObjectChangesIfSupported(AutoRepliesUtility.PARAMETER_REPLIES, StateMachineConfiguration.class, originalStateMachine::updateModel);
        if (subscribed) LOGGER.info("Listening for updates to configuration file. Will update auto-replies if updated.");

        return originalStateMachine;
    }

    private void updateModel(StateMachineConfiguration newStateMachineConfiguration) {
        this.startState = newStateMachineConfiguration.startState;
        this.states = newStateMachineConfiguration.states;
        LOGGER.info("Updated auto-replies due to configuration changes.");
    }

    public static void configureSampleAutoReply(Configuration configuration, String resourcePath) throws IOException {
        Configuration autoReplyConfiguration = Configuration.fromStream(StateMachineConfiguration.class.getResourceAsStream(resourcePath));
        configuration.setSubConfiguration(AutoRepliesUtility.PARAMETER_REPLIES, autoReplyConfiguration);

        ConsoleIo.writeLine("Configured sample auto-reply. To adjust: Save configuration, edit it, and re-run RAPTOR.");
    }

    public String startState() {
        return startState;
    }

    public Map<String, List<Transition>> states() {
        return states;
    }

    public String getStartState() {
        return startState;
    }

    public void setStartState(String startState) {
        this.startState = startState;
    }

    public Map<String, List<Transition>> getStates() {
        return states;
    }

    public void setStates(Map<String, List<Transition>> states) {
        this.states = states;
    }
}
