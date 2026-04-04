package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.configuration.Configuration;

import java.util.function.Consumer;

class NoneSendStrategy implements SerialPortSendStrategy {
    @Override
    public Consumer<byte[]> start(Configuration configuration, SerialPort port, Runnable shutDownAction, int commandSubstitutionTimeout) {
        return input -> { // Nothing to send on inputs
        };
    }
}
