package com.github.trosenkrantz.raptor.auto.reply;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class StateMachineConfigurationTest {
    @Test
    void readFromFileWithLineComment() throws IOException {
        // Read and parse JSON file
        StateMachineConfiguration config = StateMachineConfiguration.readFromFile("src/test/resources/auto-reply-with-line-comments.json");

        // Assert parsed values
        Assertions.assertEquals("login", config.startState());
        Assertions.assertEquals(2, config.states().size());
        Assertions.assertEquals(1, config.states().get("login").size());
        Assertions.assertEquals("login\n", config.states().get("login").getFirst().input());
        Assertions.assertEquals("ok\n", config.states().get("login").getFirst().output());
        Assertions.assertEquals("active", config.states().get("login").getFirst().nextState());
        Assertions.assertEquals(2, config.states().get("active").size());
    }
    @Test
    void readFromFileWithManyComments() throws IOException {
        // Read and parse JSON file
        StateMachineConfiguration config = StateMachineConfiguration.readFromFile("src/test/resources/auto-reply-with-many-comments.json");

        // Assert parsed values
        Assertions.assertEquals("login", config.startState());
        Assertions.assertEquals(2, config.states().size());
        Assertions.assertEquals(1, config.states().get("login").size());
        Assertions.assertEquals("login\n", config.states().get("login").getFirst().input());
        Assertions.assertEquals("ok\n", config.states().get("login").getFirst().output());
        Assertions.assertEquals("active", config.states().get("login").getFirst().nextState());
        Assertions.assertEquals(2, config.states().get("active").size());
    }
}