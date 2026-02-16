package com.github.trosenkrantz.raptor.auto.reply;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.CommandSubstitutor;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class StateMachineConfiguration {
    public static final String REPLIES_PATH = "/com/github/trosenkrantz/raptor/replies.json";
    public static final String SNMP_REPLIES_PATH = "/com/github/trosenkrantz/raptor/snmp-replies.json";

    private static final Logger LOGGER = Logger.getLogger(StateMachineConfiguration.class.getName());

    private String startState;
    private Map<String, List<Transition>> states;
    private Integer commandSubstitutionTimeout; // In ms

    public StateMachineConfiguration() {
    }

    /**
     * Constructor
     *
     * @param startState                 name of the start state
     * @param states                     states
     * @param commandSubstitutionTimeout timeout in ms used for command substitutions
     */
    public StateMachineConfiguration(String startState, Map<String, List<Transition>> states, int commandSubstitutionTimeout) {
        this.startState = startState;
        this.states = states;
        this.commandSubstitutionTimeout = commandSubstitutionTimeout;
    }

    public static StateMachineConfiguration fromConfiguration(final Configuration configuration) {
        StateMachineConfiguration originalStateMachine = configuration.requireObject(AutoRepliesUtility.PARAMETER_REPLIES, StateMachineConfiguration.class);
        ConsoleIo.writeLine("Loaded auto-replies configuration with " + originalStateMachine.getStates().size() + " states and " + originalStateMachine.getStates().values().stream().map(List::size).reduce(0, Integer::sum) + " transitions.");

        if (originalStateMachine.getCommandSubstitutionTimeout() == null) {
            LOGGER.warning("Auto-reply command substitution timeout is not set, setting to " + CommandSubstitutor.DEFAULT_TIMEOUT);
            originalStateMachine.commandSubstitutionTimeout = CommandSubstitutor.DEFAULT_TIMEOUT;
        }

        boolean subscribed = configuration.subscribeToObjectChangesIfSupported(AutoRepliesUtility.PARAMETER_REPLIES, StateMachineConfiguration.class, originalStateMachine::updateModel);
        if (subscribed) LOGGER.info("Listening for updates to configuration file. Will update auto-replies if updated.");

        return originalStateMachine;
    }

    private void updateModel(StateMachineConfiguration newStateMachineConfiguration) {
        this.startState = newStateMachineConfiguration.startState;
        this.states = newStateMachineConfiguration.states;
        if (newStateMachineConfiguration.commandSubstitutionTimeout != null) this.commandSubstitutionTimeout = newStateMachineConfiguration.getCommandSubstitutionTimeout();
        LOGGER.info("Updated auto-replies due to configuration changes.");
    }

    public static void configureSampleAutoReply(Configuration configuration, String resourcePath) throws IOException {

        Configuration autoReplyConfiguration = Configuration.fromStream(StateMachineConfiguration.class.getResourceAsStream(resourcePath));
        CommandSubstitutor.configureTimeout(autoReplyConfiguration);
        configuration.setSubConfiguration(AutoRepliesUtility.PARAMETER_REPLIES, autoReplyConfiguration);
        ConsoleIo.writeLine("Configured sample auto-reply. To adjust: Save configuration, edit it, and re-run RAPTOR.");
    }

    public String getStartState() {
        return startState;
    }

    public Map<String, List<Transition>> getStates() {
        return states;
    }

    public Integer getCommandSubstitutionTimeout() {
        return commandSubstitutionTimeout;
    }
}
