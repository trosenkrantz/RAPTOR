package com.github.trosenkrantz.raptor.auto.reply;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class PeakableBufferlessStateMachineTest {
    public static final int COMMAND_SUBSTITUTION_TIMEOUT = 1000;

    @Test
    void matchesInputEscapeCharacters() {
        PeakableBufferlessStateMachine machine = new PeakableBufferlessStateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(
                        new Transition("login\n", "ok", null)
                )),
                COMMAND_SUBSTITUTION_TIMEOUT
        ));

        PeakResult result = machine.peak("login\n".getBytes(StandardCharsets.US_ASCII));

        Assertions.assertTrue(result.matched());
        Assertions.assertEquals("login\n", result.matchedTransition().input());
        Assertions.assertEquals(1, result.captureGroups().size());
        Assertions.assertEquals("login\n", result.captureGroups().getFirst());
    }

    @Test
    void matchesInputHex() {
        PeakableBufferlessStateMachine machine = new PeakableBufferlessStateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(
                        new Transition("\\x00\\x01\\x02", "ok", null)
                )),
                COMMAND_SUBSTITUTION_TIMEOUT
        ));

        byte[] input = new byte[]{0, 1, 2};

        PeakResult result = machine.peak(input);

        Assertions.assertTrue(result.matched());
        Assertions.assertEquals(1, result.captureGroups().size());
        Assertions.assertEquals("\\x00\\x01\\x02", result.captureGroups().getFirst());
    }

    @Test
    void matchesInputHexRegex() {
        PeakableBufferlessStateMachine machine = new PeakableBufferlessStateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(
                        new Transition("\\x00(\\x01)+\\xff", "ok", null)
                )),
                COMMAND_SUBSTITUTION_TIMEOUT
        ));

        PeakResult result = machine.peak(new byte[]{0, 1, 1, -1});

        Assertions.assertTrue(result.matched());
        Assertions.assertEquals(2, result.captureGroups().size());
        Assertions.assertEquals("\\x00\\x01\\x01\\xff", result.captureGroups().getFirst());
        Assertions.assertEquals("\\x01", result.captureGroups().get(1));
    }

    @Test
    void matchesInputWithDots() {
        PeakableBufferlessStateMachine machine = new PeakableBufferlessStateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(
                        new Transition("1.2.3.4", "ok", null)
                )),
                COMMAND_SUBSTITUTION_TIMEOUT
        ));

        PeakResult result = machine.peak("1.2.3.4".getBytes(StandardCharsets.US_ASCII));

        Assertions.assertTrue(result.matched());
        Assertions.assertEquals(1, result.captureGroups().size());
        Assertions.assertEquals("1.2.3.4", result.captureGroups().getFirst());
    }

    @Test
    void matchesInputWithDotsRegex() {
        PeakableBufferlessStateMachine machine = new PeakableBufferlessStateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(
                        new Transition("1.2.3..+", "ok", null)
                )),
                COMMAND_SUBSTITUTION_TIMEOUT
        ));

        PeakResult result = machine.peak("1.2.3.4".getBytes(StandardCharsets.US_ASCII));

        Assertions.assertTrue(result.matched());
        Assertions.assertEquals(1, result.captureGroups().size());
        Assertions.assertEquals("1.2.3.4", result.captureGroups().getFirst());
    }

    @Test
    void detectsWhenNoMatch() {
        // Arrange
        PeakableBufferlessStateMachine machine = new PeakableBufferlessStateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(
                        new Transition("login", "ok", null)
                )),
                COMMAND_SUBSTITUTION_TIMEOUT
        ));

        // Act
        PeakResult result = machine.peak("logout".getBytes(StandardCharsets.US_ASCII));

        // Assert
        Assertions.assertFalse(result.matched());
    }

    @Test
    void switchesStates() {
        PeakableBufferlessStateMachine machine = new PeakableBufferlessStateMachine(new StateMachineConfiguration(
                "S1",
                Map.of(
                        "S1", List.of(new Transition("go", "out", "S2")),
                        "S2", List.of(new Transition("back", "out", "S1"))
                ),
                COMMAND_SUBSTITUTION_TIMEOUT
        ));

        Transition t1 = machine.peak("go".getBytes(StandardCharsets.US_ASCII)).matchedTransition();
        machine.transition(t1);

        Transition t2 = machine.peak("back".getBytes(StandardCharsets.US_ASCII)).matchedTransition();
        machine.transition(t2);

        Transition t3 = machine.peak("go".getBytes(StandardCharsets.US_ASCII)).matchedTransition();

        Assertions.assertEquals("go", t3.input());
    }

    @Test
    void firstMatchingTransitionWins() {
        PeakableBufferlessStateMachine machine = new PeakableBufferlessStateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(
                        new Transition("log.*", "regex", null),
                        new Transition("login", "exact", null)
                )),
                COMMAND_SUBSTITUTION_TIMEOUT
        ));

        Transition transition = machine.peak("login".getBytes(StandardCharsets.US_ASCII)).matchedTransition();

        Assertions.assertEquals("log.*", transition.input());
    }
}