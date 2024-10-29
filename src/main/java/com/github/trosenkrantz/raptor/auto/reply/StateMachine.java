package com.github.trosenkrantz.raptor.auto.reply;

import com.github.trosenkrantz.raptor.io.BytesFormatter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StateMachine {
    private final StateMachineConfiguration configuration;
    private final Consumer<byte[]> onOutput;

    private String currentState;
    private StringBuilder buffer = new StringBuilder();

    public StateMachine(final StateMachineConfiguration configuration, final Consumer<byte[]> onOutput) {
        this.configuration = configuration;
        currentState = configuration.startState();
        this.onOutput = onOutput;
    }

    public void onInput(byte[] input) {
        buffer.append(new String(input, StandardCharsets.ISO_8859_1)); // Use ISO 8859-1 to be able to match on arbitrary bytes

        List<Transition> transitions = configuration.states().get(currentState);
        if (transitions == null) throw new IllegalStateException("Auto-reply state " + currentState + " not defined.");

        for (Transition transition : transitions) {
            Matcher matcher = Pattern.compile(transition.input(), Pattern.DOTALL).matcher(buffer); // We expect arbitrary bytes, so we use dotall mode to treat line terminators bytes as any other bytes

            if (matcher.matches()) {
                onOutput.accept(BytesFormatter.escapedHexStringToBytes(transition.output()));
                if (transition.nextState() != null) {
                    currentState = transition.nextState();
                }
                resetInputBuffer();
                break;
            }
        }
    }

    public void resetInputBuffer() {
        buffer = new StringBuilder();
    }
}
