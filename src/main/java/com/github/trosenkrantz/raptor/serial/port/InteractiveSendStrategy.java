package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.UserAbortedException;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.io.BytesFormatter;
import com.github.trosenkrantz.raptor.io.ConsoleIo;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

class InteractiveSendStrategy implements SerialPortSendStrategy {
    private static final Logger LOGGER = Logger.getLogger(InteractiveSendStrategy.class.getName());

    @Override
    public Consumer<byte[]> start(Configuration configuration, SerialPort port, Runnable shutDownAction) {
        Thread.ofVirtual().start(() -> {
                    try {
                        while (port.isOpen()) {
                            String userAnswer = ConsoleIo.askForString("What to send", "Hello, World!"); // User answers with fully escaped string
                            byte[] whatToSend = BytesFormatter.raptorEncodingToBytes(userAnswer);
                            SerialPortUtility.writeToPort(port, whatToSend);
                        }
                    } catch (UserAbortedException ignore) {
                        shutDownAction.run();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error occurred.", e);
                        shutDownAction.run();
                    } finally {
                        try {
                            port.closePort();
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed closing port.", e);
                            shutDownAction.run();
                        }
                    }
                }
        );
        return input -> { // Nothing to send on inputs
        };
    }
}
