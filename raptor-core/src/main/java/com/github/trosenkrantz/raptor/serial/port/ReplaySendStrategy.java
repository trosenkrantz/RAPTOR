package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.replay.ReplaySession;
import com.github.trosenkrantz.raptor.replay.ReplaySettings;

import java.util.function.Consumer;

class ReplaySendStrategy implements SerialPortSendStrategy {
    private ReplaySettings.Loaded replay;

    @Override
    public void configure(Configuration configuration) {
        ReplaySettings.configure(configuration);
    }

    @Override
    public void load(Configuration configuration) {
        replay = ReplaySettings.load(configuration);
    }

    @Override
    public Consumer<byte[]> start(Configuration configuration, SerialPort port, Runnable shutDownAction, int commandSubstitutionTimeout) {
        new ReplaySession(replay.timeline(), replay.mode(), port::isOpen).startInBackground(event -> {
            SerialPortUtility.writeToPort(port, event.data());
        });
        return input -> {
            // Replay ignores incoming data.
        };
    }
}
