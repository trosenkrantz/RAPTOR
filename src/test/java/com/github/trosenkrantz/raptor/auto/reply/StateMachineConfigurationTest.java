package com.github.trosenkrantz.raptor.auto.reply;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StateMachineConfigurationTest {
    @Test
    void readFromFileWithLineComment() throws IOException {
        // Read and parse JSON file
        StateMachineConfiguration config = StateMachineConfiguration.readFromFile("src/test/resources/auto-reply-with-line-comments.json");

        // Assert parsed values
        assertEquals("login", config.startState());
        assertEquals(2, config.states().size());
        assertEquals(1, config.states().get("login").size());
        assertEquals("login\n", config.states().get("login").getFirst().input());
        assertEquals("ok\n", config.states().get("login").getFirst().output());
        assertEquals("active", config.states().get("login").getFirst().nextState());
        assertEquals(2, config.states().get("active").size());
    }
    @Test
    void readFromFileWithManyComments() throws IOException {
        // Read and parse JSON file
        StateMachineConfiguration config = StateMachineConfiguration.readFromFile("src/test/resources/auto-reply-with-many-comments.json");

        // Assert parsed values
        assertEquals("login", config.startState());
        assertEquals(2, config.states().size());
        assertEquals(1, config.states().get("login").size());
        assertEquals("login\n", config.states().get("login").getFirst().input());
        assertEquals("ok\n", config.states().get("login").getFirst().output());
        assertEquals("active", config.states().get("login").getFirst().nextState());
        assertEquals(2, config.states().get("active").size());
    }
}