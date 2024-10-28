package com.github.trosenkrantz.raptor.auto.reply;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class StateMachineTest {
    @Test
    void matchesInputEscapeCharacters() {
        // Arrange
        List<byte[]> capturedOutputs = new ArrayList<>();
        StateMachine stateMachine = new StateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(new Transition("login\n", "ok", null)))
        ), capturedOutputs::add);

        // Act
        stateMachine.onInput("login".getBytes());
        Assertions.assertTrue(capturedOutputs.isEmpty()); // Still no output
        stateMachine.onInput("\n".getBytes());

        // Assert
        Assertions.assertEquals(1, capturedOutputs.size());
        Assertions.assertArrayEquals("ok".getBytes(), capturedOutputs.getFirst());
    }

    @Test
    void matchesInputHex() {
        // Arrange
        List<byte[]> capturedOutputs = new ArrayList<>();
        StateMachine stateMachine = new StateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(new Transition("\\x00login", "ok", null)))
        ), capturedOutputs::add);

        // Act
        stateMachine.onInput(new byte[]{0});
        stateMachine.onInput("logi".getBytes());
        Assertions.assertTrue(capturedOutputs.isEmpty()); // Still no output
        stateMachine.onInput("n".getBytes());

        // Assert
        Assertions.assertEquals(1, capturedOutputs.size());
        Assertions.assertArrayEquals("ok".getBytes(), capturedOutputs.getFirst());
    }

    @Test
    void matchesInputHexRegex() {
        // Arrange
        List<byte[]> capturedOutputs = new ArrayList<>();
        StateMachine stateMachine = new StateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(new Transition("\\x00(\\x01)+\\xff", "ok", null)))
        ), capturedOutputs::add);

        // Act
        stateMachine.onInput(new byte[]{0, 1, 1});
        Assertions.assertTrue(capturedOutputs.isEmpty()); // Still no output
        stateMachine.onInput(new byte[]{-1});

        // Assert
        Assertions.assertEquals(1, capturedOutputs.size());
        Assertions.assertArrayEquals("ok".getBytes(), capturedOutputs.getFirst());
    }

    @Test
    void matchesInputWithDots() {
        // Arrange
        List<byte[]> capturedOutputs = new ArrayList<>();
        StateMachine stateMachine = new StateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(new Transition("1.2.3.4", "ok", null)))
        ), capturedOutputs::add);

        // Act
        stateMachine.onInput("1.2.3.".getBytes());
        Assertions.assertTrue(capturedOutputs.isEmpty()); // Still no output
        stateMachine.onInput("4".getBytes());

        // Assert
        Assertions.assertEquals(1, capturedOutputs.size());
        Assertions.assertArrayEquals("ok".getBytes(), capturedOutputs.getFirst());
    }

    @Test
    void matchesInputWithDotsRegex() {
        // Arrange
        List<byte[]> capturedOutputs = new ArrayList<>();
        StateMachine stateMachine = new StateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(new Transition("1.2.3..+", "ok", null)))
        ), capturedOutputs::add);

        // Act
        stateMachine.onInput("1.2.3.".getBytes());
        Assertions.assertTrue(capturedOutputs.isEmpty()); // Still no output
        stateMachine.onInput("4".getBytes());

        // Assert
        Assertions.assertEquals(1, capturedOutputs.size());
        Assertions.assertArrayEquals("ok".getBytes(), capturedOutputs.getFirst());
    }

    @Test
    void formatsOutputEscapeCharacters() {
        // Arrange
        List<byte[]> capturedOutputs = new ArrayList<>();
        StateMachine stateMachine = new StateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(new Transition("login", "ok\n", null)))
        ), capturedOutputs::add);

        // Act
        stateMachine.onInput("login".getBytes());

        // Assert
        Assertions.assertEquals(1, capturedOutputs.size());
        Assertions.assertArrayEquals("ok\n".getBytes(), capturedOutputs.getFirst());
    }

    @Test
    void formatsOutputHex() {
        // Arrange
        List<byte[]> capturedOutputs = new ArrayList<>();
        StateMachine stateMachine = new StateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(new Transition("login", "ok\\x02", null)))
        ), capturedOutputs::add);

        // Act
        stateMachine.onInput("login".getBytes());

        // Assert
        Assertions.assertEquals(1, capturedOutputs.size());
        Assertions.assertArrayEquals(new byte[] {'o', 'k', 2}, capturedOutputs.getFirst());
    }

    @Test
    void switchesStates() {
        // Arrange
        List<byte[]> capturedOutputs = new ArrayList<>();
        StateMachine stateMachine = new StateMachine(new StateMachineConfiguration(
                "S1",
                Map.of(
                        "S1", List.of(new Transition("S1 input", "S1 output", "S2")),
                        "S2", List.of(new Transition("S2 input", "S2 output", "S1"))
                )
        ), capturedOutputs::add);

        // Act and Assert
        stateMachine.onInput("S1 input".getBytes());
        Assertions.assertEquals(1, capturedOutputs.size());
        Assertions.assertArrayEquals("S1 output".getBytes(), capturedOutputs.getFirst());
        stateMachine.onInput("S2 input".getBytes());
        Assertions.assertEquals(2, capturedOutputs.size());
        Assertions.assertArrayEquals("S2 output".getBytes(), capturedOutputs.get(1));
        stateMachine.onInput("S1 input".getBytes());
        Assertions.assertEquals(3, capturedOutputs.size());
        Assertions.assertArrayEquals("S1 output".getBytes(), capturedOutputs.get(2));
    }
}