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
        Thread.ofVirtual().start(() -> {
                    Supplier<byte[]> supplier = () -> {
                        String userAnswer = ConsoleIo.askForString("What to send", "Hello, World!"); // User answers with fully escaped string
                        return BytesFormatter.fullyEscapedStringToBytes(userAnswer);
                    };

                    try {
                        // Keep sending while the socket is open
                        // While prompting user for input, the socket may have been closed, which is why we check it just before sending
                        byte[] bytesToSend = supplier.get();
                        while (socket.isOpen()) {
                            WebSocketService.send(socket, bytesToSend);
                            bytesToSend = supplier.get();
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
