package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.Configuration;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public interface TcpSendStrategy {
    default void load(final Configuration configuration) throws IOException {
    }
    Consumer<byte[]> initialise(final Socket socket, final Runnable shutDownAction) throws IOException;
}
