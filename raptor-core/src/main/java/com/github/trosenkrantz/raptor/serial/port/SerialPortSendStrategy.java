package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.configuration.Configuration;

import java.util.function.Consumer;

public interface SerialPortSendStrategy {
    default void configure(Configuration configuration) throws Exception {
    }

    /** To be called once when running the service. */
    default void load(Configuration configuration) {
    }

    /**
     * This is called when the serial port is opened.
     *
     * @param configuration  configuration
     * @param port           the serial port
     * @param shutDownAction to be called by the strategy if it wants to shut down
     * @return a consumer to be called when the serial port receives data
     */
    Consumer<byte[]> start(Configuration configuration, SerialPort port, Runnable shutDownAction, int commandSubstitutionTimeout);
}
