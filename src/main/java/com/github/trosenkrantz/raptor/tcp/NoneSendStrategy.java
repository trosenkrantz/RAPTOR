package com.github.trosenkrantz.raptor.tcp;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

class NoneSendStrategy implements TcpSendStrategy {
    @Override
    public Consumer<byte[]> initialise(Socket socket, Runnable shutDownAction) throws IOException {
        return input -> { // Nothing to send on inputs
        };
    }
}
