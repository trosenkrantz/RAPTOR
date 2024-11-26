package com.github.trosenkrantz.raptor.web.socket;

import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import org.java_websocket.WebSocket;

import java.util.function.Consumer;
import java.util.function.Supplier;

class InteractiveSendStrategy implements WebSocketSendStrategy {
    @Override
    public Consumer<byte[]> initialise(WebSocket socket, Runnable shutDownAction) {
        Supplier<Runnable> sendActionSupplier = () -> {
            String userAnswer = ConsoleIo.askForString("What to send", "Hello, World!"); // User answers with fully escaped string
            byte[] userAnswerAsBytes = BytesFormatter.fullyEscapedStringToBytes(userAnswer);
            return () -> WebSocketService.send(socket, userAnswerAsBytes);
        };

        Thread.ofVirtual().start(() -> {
                    try {
                        Runnable sendAction = sendActionSupplier.get();
                        while (socket.isOpen()) {
                            sendAction.run();
                            sendAction = sendActionSupplier.get();
                        }
                    } catch (AbortedException ignore) {
                        shutDownAction.run();
                    } catch (Exception e) {
                        ConsoleIo.writeException(e);
                        shutDownAction.run();
                    } finally {
                        try {
                            socket.close();
                        } catch (Exception e) {
                            ConsoleIo.writeException(e);
                            shutDownAction.run();
                        }
                    }
                }
        );
        return input -> { // Nothing to send on inputs
        };
    }
}
