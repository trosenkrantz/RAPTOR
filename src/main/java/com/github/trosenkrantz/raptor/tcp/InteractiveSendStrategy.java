package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.AbortedException;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.OutputStream;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

class InteractiveSendStrategy implements TcpSendStrategy {
    private static final Logger LOGGER = Logger.getLogger(InteractiveSendStrategy.class.getName());

    @Override
    public Consumer<byte[]> initialise(Socket socket, Runnable shutDownAction) {
        Thread.ofVirtual().start(() -> {
                    try {
                        Supplier<byte[]> supplier = () -> BytesFormatter.fullyEscapedStringToBytes(ConsoleIo.askForString("What to send", "Hello, World!"));

                        OutputStream out = socket.getOutputStream();
                        byte[] whatToSends = supplier.get();
                        while (!socket.isInputShutdown()) {
                            out.write(whatToSends);
                            LOGGER.info("Sent " + BytesFormatter.bytesToFullyEscapedStringWithType(whatToSends));

                            whatToSends = supplier.get();
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
