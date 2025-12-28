package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.configuration.Configuration;

import java.io.IOException;
import java.util.function.Consumer;

class NoneSendStrategy implements SerialPortSendStrategy {
    @Override
    public Consumer<byte[]> start(Configuration configuration, SerialPort port, Runnable shutDownAction) throws IOException {
        return input -> { // Nothing to send on inputs
        };
    }
}
