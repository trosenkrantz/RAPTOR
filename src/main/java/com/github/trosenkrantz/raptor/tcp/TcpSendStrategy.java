package com.github.trosenkrantz.raptor.tcp;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public interface TcpSendStrategy {
    Consumer<byte[]> initialise(final Socket socket) throws IOException;
}
