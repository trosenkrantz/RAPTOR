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

        Optional<Transition> transition = machine.peak("login\n".getBytes(StandardCharsets.US_ASCII));

        Assertions.assertTrue(transition.isPresent());
        Assertions.assertEquals("login\n", transition.get().input());
    }

    @Test
    void matchesInputHex() {
        PeakableBufferlessStateMachine machine = new PeakableBufferlessStateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(
                        new Transition("\\x00login", "ok", null)
                )),
                COMMAND_SUBSTITUTION_TIMEOUT
        ));

        byte[] input = new byte[]{0, 'l', 'o', 'g', 'i', 'n'};

        Optional<Transition> transition = machine.peak(input);

        Assertions.assertTrue(transition.isPresent());
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

        byte[] input = new byte[]{0, 1, 1, -1};

        Optional<Transition> transition = machine.peak(input);

        Assertions.assertTrue(transition.isPresent());
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

        Optional<Transition> transition =
                machine.peak("1.2.3.4".getBytes(StandardCharsets.US_ASCII));

        Assertions.assertTrue(transition.isPresent());
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

        Optional<Transition> transition = machine.peak("1.2.3.4".getBytes(StandardCharsets.US_ASCII));

        Assertions.assertTrue(transition.isPresent());
    }

    @Test
    void returnsEmptyWhenNoMatch() {
        PeakableBufferlessStateMachine machine = new PeakableBufferlessStateMachine(new StateMachineConfiguration(
                "S1",
                Map.of("S1", List.of(
                        new Transition("login", "ok", null)
                )),
                COMMAND_SUBSTITUTION_TIMEOUT
        ));

        Optional<Transition> transition = machine.peak("logout".getBytes(StandardCharsets.US_ASCII));

        Assertions.assertTrue(transition.isEmpty());
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

        Transition t1 = machine.peak("go".getBytes(StandardCharsets.US_ASCII)).orElseThrow();
        machine.transition(t1);

        Transition t2 = machine.peak("back".getBytes(StandardCharsets.US_ASCII)).orElseThrow();
        machine.transition(t2);

        Transition t3 = machine.peak("go".getBytes(StandardCharsets.US_ASCII)).orElseThrow();

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

        Transition transition = machine.peak("login".getBytes(StandardCharsets.US_ASCII)).orElseThrow();

        Assertions.assertEquals("log.*", transition.input());
    }
}