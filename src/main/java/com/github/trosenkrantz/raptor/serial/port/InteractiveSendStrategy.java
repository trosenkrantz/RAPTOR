package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.Configuration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;
import com.github.trosenkrantz.raptor.web.socket.WebSocketSendStrategy;
import com.github.trosenkrantz.raptor.web.socket.WebSocketService;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

class InteractiveSendStrategy implements SerialPortSendStrategy {
    @Override
    public Consumer<byte[]> start(Configuration configuration, SerialPort port, Runnable shutDownAction) {
        Thread.ofVirtual().start(() -> {
                    try {
                        while (port.isOpen()) {
                            String userAnswer = ConsoleIo.askForString("What to send", "Hello, World!"); // User answers with fully escaped string
                            byte[] userAnswerAsBytes = BytesFormatter.fullyEscapedStringToBytes(userAnswer);
                            port.writeBytes(userAnswerAsBytes, userAnswerAsBytes.length);
                        }
                    } catch (AbortedException ignore) {
                        shutDownAction.run();
                    } catch (Exception e) {
                        ConsoleIo.writeException(e);
                        shutDownAction.run();
                    } finally {
                        try {
                            port.closePort();
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
