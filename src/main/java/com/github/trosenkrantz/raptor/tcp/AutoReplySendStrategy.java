package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.auto.reply.StateMachine;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Logger;

class AutoReplySendStrategy implements TcpSendStrategy {
    private static final Logger LOGGER = Logger.getLogger(AutoReplySendStrategy.class.getName());

    private StateMachineConfiguration stateMachineConfiguration;

    @Override
    public void load(Configuration configuration) throws IOException {
        // Read state machine immediately to provide early feedback
        stateMachineConfiguration = StateMachineConfiguration.readFromFile(configuration.requireString(TcpUtility.PARAMETER_REPLY_FILE));
    }

    @Override
    public Consumer<byte[]> start(Socket socket, Runnable shutDownAction) throws IOException {
        OutputStream out = socket.getOutputStream();
        StateMachine stateMachine = new StateMachine(stateMachineConfiguration, output -> {
            try {
                out.write(output);
                LOGGER.info("Sent " + BytesFormatter.bytesToFullyEscapedStringWithType(output));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return input -> {
            for (byte b : input) {
                stateMachine.onInput(new byte[]{b}); // Pass on byte by byte
            }
        };
    }
}
