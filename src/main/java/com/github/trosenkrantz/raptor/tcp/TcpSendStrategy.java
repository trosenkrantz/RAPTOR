package com.github.trosenkrantz.raptor.tcp;

import com.github.trosenkrantz.raptor.configuration.Configuration;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public interface TcpSendStrategy {
    /**
     * To be called once when running the service
     *
     * @param configuration configuration to load
     * @throws IOException if an I/O error occurs
     */
    default void load(final Configuration configuration) throws IOException {
    }

    /**
     * To be called whenever the service opens a new connection
     * @param socket socket to send messages to
     * @param shutDownAction action that the send strategy will call when the service should shut down, e.g., if aborted by the user
     * @return a consumer that will be called with the socket has received data
     * @throws IOException if an I/O error occurs
     */
    Consumer<byte[]> start(final Socket socket, final Runnable shutDownAction, final int commandSubstitutionTimeout) throws IOException;
}
