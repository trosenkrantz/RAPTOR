package com.github.trosenkrantz.raptor.tcp;

import java.net.Socket;
import java.util.function.Consumer;

class NoneSendStrategy implements TcpSendStrategy {
    @Override
    public Consumer<byte[]> start(Socket socket, Runnable shutDownAction, int commandSubstitutionTimeout) {
        return input -> { // Nothing to send on inputs
        };
    }
}
