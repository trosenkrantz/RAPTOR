package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.auto.reply.StateMachine;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.function.Consumer;

class AutoReplySendStrategy implements WebSocketSendStrategy {
    private StateMachineConfiguration stateMachineConfiguration;

    @Override
    public void load(Configuration configuration) throws IOException {
        // Read state machine immediately to provide early feedback
        stateMachineConfiguration = StateMachineConfiguration.readFromFile(configuration.requireString(WebSocketService.PARAMETER_REPLY_FILE));
    }

    @Override
    public Consumer<byte[]> initialise(WebSocket socket, Runnable shutDownAction) {
        StateMachine stateMachine = new StateMachine(stateMachineConfiguration, output -> WebSocketService.send(socket, output));
        return input -> {
            for (byte b : input) {
                stateMachine.onInput(new byte[]{b}); // Pass on byte by byte
            }
            stateMachine.resetInputBuffer(); // Reset as we process whole WebSocket frames at a time
        };
    }
}
