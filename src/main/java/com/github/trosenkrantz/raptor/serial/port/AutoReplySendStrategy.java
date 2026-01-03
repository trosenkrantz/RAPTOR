package com.github.trosenkrantz.raptor.serial.port;

import com.fazecast.jSerialComm.SerialPort;
import com.github.trosenkrantz.raptor.configuration.Configuration;
import com.github.trosenkrantz.raptor.auto.reply.StateMachine;
import com.github.trosenkrantz.raptor.auto.reply.StateMachineConfiguration;
import com.github.trosenkrantz.raptor.web.socket.WebSocketService;

import java.io.IOException;
import java.util.function.Consumer;

class AutoReplySendStrategy implements SerialPortSendStrategy {
    @Override
    public Consumer<byte[]> start(Configuration configuration, SerialPort port, Runnable shutDownAction) throws IOException {
        StateMachine stateMachine = new StateMachine(
                StateMachineConfiguration.fromConfiguration(configuration),
                payload -> SerialPortUtility.writeToPort(port, payload)
        );

        return input -> {
            for (byte b : input) {
                stateMachine.onInput(new byte[]{b}); // Pass on byte by byte
            }
        };
    }
}
