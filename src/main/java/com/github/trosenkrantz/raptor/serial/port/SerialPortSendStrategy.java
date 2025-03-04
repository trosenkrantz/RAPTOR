package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.Configuration;

import java.io.IOException;
import java.util.function.Consumer;

public interface SerialPortSendStrategy {
    Consumer<byte[]> start(final Configuration configuration, final SerialPort port, final Runnable shutDownAction) throws IOException;
}
