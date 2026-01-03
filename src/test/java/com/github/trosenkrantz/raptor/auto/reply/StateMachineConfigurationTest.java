package com.github.trosenkrantz.raptor.auto.reply;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class StateMachineConfigurationTest {
    @Test
    void readFromFileWithLineComment() throws IOException {
        // Read and parse JSON file
        Configuration autoReplyConfiguration = Configuration.fromStream(StateMachineConfigurationTest.class.getResourceAsStream("/auto-reply-with-line-comments.json"));
        Configuration configuration = Configuration.empty();
        configuration.setSubConfiguration(AutoRepliesUtility.PARAMETER_REPLIES, autoReplyConfiguration);
        StateMachineConfiguration stateMachineConfiguration = configuration.getObject(AutoRepliesUtility.PARAMETER_REPLIES, StateMachineConfiguration.class);

        // Assert parsed values
        Assertions.assertEquals("login", stateMachineConfiguration.startState());
        Assertions.assertEquals(2, stateMachineConfiguration.states().size());
        Assertions.assertEquals(1, stateMachineConfiguration.states().get("login").size());
        Assertions.assertEquals("login\n", stateMachineConfiguration.states().get("login").getFirst().input());
        Assertions.assertEquals("ok\n", stateMachineConfiguration.states().get("login").getFirst().output());
        Assertions.assertEquals("active", stateMachineConfiguration.states().get("login").getFirst().nextState());
        Assertions.assertEquals(2, stateMachineConfiguration.states().get("active").size());
    }

    @Test
    void readFromFileWithManyComments() throws IOException {
        // Read and parse JSON file
        Configuration autoReplyConfiguration = Configuration.fromStream(StateMachineConfigurationTest.class.getResourceAsStream("/auto-reply-with-many-comments.json"));
        Configuration configuration = Configuration.empty();
        configuration.setSubConfiguration(AutoRepliesUtility.PARAMETER_REPLIES, autoReplyConfiguration);
        StateMachineConfiguration stateMachineConfiguration = configuration.getObject(AutoRepliesUtility.PARAMETER_REPLIES, StateMachineConfiguration.class);

        // Assert parsed values
        Assertions.assertEquals("login", stateMachineConfiguration.startState());
        Assertions.assertEquals(2, stateMachineConfiguration.states().size());
        Assertions.assertEquals(1, stateMachineConfiguration.states().get("login").size());
        Assertions.assertEquals("login\n", stateMachineConfiguration.states().get("login").getFirst().input());
        Assertions.assertEquals("ok\n", stateMachineConfiguration.states().get("login").getFirst().output());
        Assertions.assertEquals("active", stateMachineConfiguration.states().get("login").getFirst().nextState());
        Assertions.assertEquals(2, stateMachineConfiguration.states().get("active").size());
    }
}