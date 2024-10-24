package com.github.trosenkrantz.raptor.auto.reply;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class StateMachineTest {
    @Test
    void name() {
        // Arrange
        List<byte[]> capturedOutputs = new ArrayList<>();
        StateMachine stateMachine = new StateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(new Transition("\\x00login\n", "\\x00ok\n", "S1")))
        ), capturedOutputs::add);

        // Act
        stateMachine.onInput(new byte[]{0});
        stateMachine.onInput("login".getBytes());
        Assertions.assertTrue(capturedOutputs.isEmpty()); // Still no output
        stateMachine.onInput("\n".getBytes());

        // Assert
        Assertions.assertEquals(1, capturedOutputs.size());
        Assertions.assertArrayEquals("\\x00ok\n".getBytes(), capturedOutputs.getFirst());
    }
}