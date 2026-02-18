package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.UserAbortedException;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.CommandSubstitutor;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

class InteractiveSendStrategy implements WebSocketSendStrategy {
    private static final Logger LOGGER = Logger.getLogger(InteractiveSendStrategy.class.getName());

    private int commandSubstitutionTimeout;

    @Override
    public void configure(Configuration configuration) throws IOException {
        CommandSubstitutor.TIMEOUT_SETTING.configure(configuration);
    }

    @Override
    public void load(Configuration configuration) {
        this.commandSubstitutionTimeout = CommandSubstitutor.TIMEOUT_SETTING.readAndRequireOrDefault(configuration);
    }

    @Override
    public Consumer<byte[]> initialise(WebSocket socket, Runnable shutDownAction) {
        Thread.ofVirtual().start(() -> {
                    Supplier<byte[]> supplier = () -> {
                        String userAnswer = ConsoleIo.askForString("What to send", "Hello, World!"); // User answers with fully escaped string
                        return BytesFormatter.raptorEncodingToBytes(userAnswer, commandSubstitutionTimeout);
                    };

                    try {
                        // Keep sending while the socket is open
                        // While prompting user for input, the socket may have been closed, which is why we check it just before sending
                        byte[] bytesToSend = supplier.get();
                        while (socket.isOpen()) {
                            WebSocketService.send(socket, bytesToSend);
                            bytesToSend = supplier.get();
                        }
                    } catch (UserAbortedException ignore) {
                        shutDownAction.run();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error occurred.", e);
                        shutDownAction.run();
                    } finally {
                        try {
                            socket.close();
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed closing socket.", e);
                            shutDownAction.run();
                        }
                    }
                }
        );
        return input -> { // Nothing to send on inputs
        };
    }
}
