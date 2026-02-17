package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.UserAbortedException;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.CommandSubstitutor;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

class InteractiveSendStrategy implements TcpSendStrategy {
    private static final Logger LOGGER = Logger.getLogger(InteractiveSendStrategy.class.getName());
    private int commandSubstitutionTimeout;

    @Override
    public void configure(Configuration configuration) throws IOException {
        CommandSubstitutor.configureTimeout(configuration);
    }

    @Override
    public void load(Configuration configuration) throws IOException {
        this.commandSubstitutionTimeout = CommandSubstitutor.requireTimeout(configuration);
    }

    @Override
    public Consumer<byte[]> start(Socket socket, Runnable shutDownAction) {
        Thread.ofVirtual().start(() -> {
                    try {
                        Supplier<byte[]> supplier = () -> BytesFormatter.raptorEncodingToBytes(ConsoleIo.askForString("What to send", "Hello, World!"), commandSubstitutionTimeout);

                        OutputStream out = socket.getOutputStream();
                        byte[] whatToSend = supplier.get();
                        while (!socket.isInputShutdown()) {
                            out.write(whatToSend);
                            LOGGER.info("Sent " + BytesFormatter.bytesToRaptorEncodingWithType(whatToSend));

                            whatToSend = supplier.get();
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
