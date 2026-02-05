package com.github.trosenkrantz.raptor.auto.reply;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeakableBufferlessStateMachine {
    private final StateMachineConfiguration configuration;

    private String currentStateName;

    public PeakableBufferlessStateMachine(final StateMachineConfiguration configuration) {
        this.configuration = configuration;
        currentStateName = configuration.startState();
    }

    public Optional<Transition> peak(byte[] input) {
        List<Transition> currentState = configuration.states().get(currentStateName);
        if (currentState == null) throw new IllegalStateException("Auto-reply state " + currentStateName + " not defined.");

        String instanceBuffer = new String(input, StandardCharsets.ISO_8859_1); // Use ISO 8859-1 to be able to match on arbitrary bytes

        for (Transition transition : currentState) {
            Matcher matcher = Pattern.compile(transition.input(), Pattern.DOTALL).matcher(instanceBuffer); // We expect arbitrary bytes, so we use dotall mode to treat line terminators bytes as any other bytes

            if (matcher.matches()) return Optional.of(transition);
        }

        return Optional.empty();
    }

    public void transition(Transition transition) {
        if (transition.nextState() != null) {
            currentStateName = transition.nextState();
        }
    }
}
