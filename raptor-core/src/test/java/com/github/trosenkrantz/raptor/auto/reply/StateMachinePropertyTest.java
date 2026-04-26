package com.github.trosenkrantz.raptor.auto.reply;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

class StateMachinePropertyTest {
    public static final int COMMAND_SUBSTITUTION_TIMEOUT = 1000;

    @Property
    void stateMachineDoesNotCrashOnArbitraryInput(@ForAll @Size(max = 100) List<byte[]> inputs) {
        // Construct a simple state machine to test
        StateMachine stateMachine = new StateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(new Transition(".*", "ok", "S1"))),
                COMMAND_SUBSTITUTION_TIMEOUT
        ), (output) -> {}); // No-op consumer

        // Act
        for (byte[] input : inputs) {
            stateMachine.onInput(input);
        }
        
        // Assert: If it reaches here without an exception, test passes.
    }
}
