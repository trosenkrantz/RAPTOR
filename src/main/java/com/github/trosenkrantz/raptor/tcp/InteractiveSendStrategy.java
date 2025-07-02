package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.OutputStream;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

class InteractiveSendStrategy implements TcpSendStrategy {
    private static final Logger LOGGER = Logger.getLogger(InteractiveSendStrategy.class.getName());

    @Override
    public Consumer<byte[]> start(Socket socket, Runnable shutDownAction) {
        Thread.ofVirtual().start(() -> {
                    try {
                        Supplier<byte[]> supplier = () -> BytesFormatter.fullyEscapedStringToBytes(ConsoleIo.askForString("What to send", "Hello, World!"));

                        OutputStream out = socket.getOutputStream();
                        byte[] whatToSend = supplier.get();
                        while (!socket.isInputShutdown()) {
                            out.write(whatToSend);
                            LOGGER.info("Sent " + BytesFormatter.bytesToFullyEscapedStringWithType(whatToSend));

                            whatToSend = supplier.get();
                        }
                    } catch (AbortedException ignore) {
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
