package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.configuration.Configuration;

import java.io.IOException;
import java.util.function.Consumer;

public interface SerialPortSendStrategy {
    /**
     * This is called when the serial port is opened.
     *
     * @param configuration  configuration
     * @param port           the serial port
     * @param shutDownAction to be called by the strategy if it wants to shut down
     * @return a consumer to be called when the serial port receives data
     * @throws IOException if an I/O error occurs
     */
    Consumer<byte[]> start(final Configuration configuration, final SerialPort port, final Runnable shutDownAction) throws IOException;
}
